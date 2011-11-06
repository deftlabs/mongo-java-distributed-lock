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
import com.deftlabs.lock.mongo.DistributedLock;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.deftlabs.lock.mongo.DistributedLockException;

// Mongo
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

// Java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

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
                                    final DistributedLockOptions pLockOptions)
    {
        try {
            _lock.lock();

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
        try {
            _lock.lock();

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
        _running.set(true);
        try {
            _lock.lock();

            _mongo = new Mongo(new MongoURI(_options.getMongoUri()));

            // Init the db/collection.
            LockDao.setup(_mongo, _options);
            if (_options.getEnableHistory()) LockHistoryDao.setup(_mongo, _options);

            // Init the monitor threads.
            _lockHeartbeat = new Monitor.LockHeartbeat(_mongo, _options, _locks);
            (new Thread(_lockHeartbeat)).start();

            _lockTimeout = new Monitor.LockTimeout(_mongo, _options);
            (new Thread(_lockTimeout)).start();


        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    /**
     * Initialize the service.
     */
    @Override
    public void shutdown() {

        if (!_running.get()) throw new IllegalStateException("shutdown called but not running");
        _running.set(false);

        try {
            _lock.lock();

            // Interrupt the locks.
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);
                if (lock == null) continue;

                ((LockImpl)lock).destroy();
            }

            _lockTimeout.stopRunning();
            _lockHeartbeat.stopRunning();

        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    @Override
    public boolean isRunning() { return _running.get(); }

    private Mongo _mongo;

    private final ReentrantLock _lock = new ReentrantLock(true);
    private final DistributedLockSvcOptions _options;

    private final AtomicBoolean _running = new AtomicBoolean(false);

    private Monitor.LockHeartbeat _lockHeartbeat;
    private Monitor.LockTimeout _lockTimeout;

    private final Map<String, DistributedLock> _locks = new ConcurrentHashMap<String, DistributedLock>();

}

