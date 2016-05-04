/**
 * Copyright 2011, Deft Labs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deftlabs.lock.mongo.impl;

// Lib

import com.deftlabs.lock.mongo.*;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoURI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

// Mongo
// Java

/**
 * The distributed lock server implementation.
 */
public final class SvcImpl implements DistributedLockSvc {

    public SvcImpl(final DistributedLockSvcOptions pOptions) {
        _options = pOptions;
    }

    /**
     * Returns a new lock. If the name is already in use and the lock has been
     * created, it returns the existing lock.
     */
    @Override
    public DistributedLock create(  final String pLockName,
                                    final DistributedLockOptions pLockOptions) {
        if ( !isRunning() ) throw new IllegalStateException("cannot create lock when service not running");

        _lock.lock();
        try {
            if (_locks.containsKey(pLockName)) return _locks.get(pLockName);

            final LockImpl lock = new LockImpl(_mongo, pLockName, pLockOptions, _options);

            lock.init();

            _locks.put(pLockName, lock);

            return lock;

        } finally { _lock.unlock(); }
    }

    /**
     * Returns a new lock. If the name is already in use and the lock has been
     * created, it returns the existing lock.
     */
    @Override
    public DistributedLock create(final String pLockName) {
        return create(pLockName, new DistributedLockOptions());
    }

    @Override
    public void destroy(final DistributedLock pLock) {
        _lock.lock();
        try {
            if (!_locks.containsKey(pLock.getName()))
            { throw new DistributedLockException("Lock has already been destroyed: " + pLock.getName()); }

            // Make sure the lock isn't locked.
            if (pLock.isLocked())
            { throw new IllegalStateException("Lock is currently in use - must unlock before destroying"); }

            try { ((LockImpl)pLock).destroy(); } finally { _locks.remove(pLock.getName()); }

        } finally { _lock.unlock(); }
    }

    /**
     * Initialize the service.
     */
    @Override
    public void startup() {
        if (!_running.compareAndSet(false, true)) throw new IllegalStateException("startup called but already running");

        _lock.lock();
        try {
            if(_options.getMongoClient() == null) {
                _mongo = new MongoClient(new MongoClientURI(_options.getMongoUri()));
            } else {
                _mongo = _options.getMongoClient();
            }

            // Init the db/collection.
            LockDao.setup(_mongo, _options);
            if (_options.getEnableHistory()) LockHistoryDao.setup(_mongo, _options);

            // Init the monitor threads.
            _lockHeartbeat = new Monitor.LockHeartbeat(_mongo, _options, _locks);
            _lockHeartbeat.start();

            _lockTimeout = new Monitor.LockTimeout(_mongo, _options);
            _lockTimeout.start();

            _lockUnlocked = new Monitor.LockUnlocked(_mongo, _options, _locks);
            _lockUnlocked.start();


        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    /**
     * Initialize the service.
     */
    @Override
    public void shutdown() {
        if (!_running.compareAndSet(true, false)) throw new IllegalStateException("shutdown called but not running");

        _lock.lock();
        try {
            // Interrupt the locks.
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);
                if (lock == null) continue;
                ((LockImpl)lock).destroy();
            }

            _lockTimeout.shutdown();
            _lockHeartbeat.shutdown();
            _lockUnlocked.shutdown();

            _locks.clear();

            if(_options.getMongoClient() == null) {
                _mongo.close();
            }
        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    @Override
    public boolean isRunning() { return _running.get(); }

    private MongoClient _mongo;

    private final ReentrantLock _lock = new ReentrantLock(true);
    private final DistributedLockSvcOptions _options;

    private final AtomicBoolean _running = new AtomicBoolean(false);

    private Monitor.LockHeartbeat _lockHeartbeat;
    private Monitor.LockTimeout _lockTimeout;
    private Monitor.LockUnlocked _lockUnlocked;

    private final Map<String, DistributedLock> _locks = new ConcurrentHashMap<String, DistributedLock>();

}

