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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The distributed lock server implementation.
 */
public final class SvcImpl implements DistributedLockSvc {

    private SvcImpl(final DistributedLockSvcOptions pOptions) {
        _options = pOptions;
    }

    @Override
    public DistributedLockSvcOptions getSvcOptions() {
        return _options.copy();
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
        _lock.lock();
        try {

            _mongo = new Mongo(new MongoURI(_options.getMongoUri()));

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

            _running.set( true );
        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    /**
     * Initialize the service.
     */
    @Override
    public void shutdown() {
        _lock.lock();
        try {

            // Performed while holding lock for finalize behavior
            if (!_running.compareAndSet(true, false)) throw new IllegalStateException("shutdown called but not running");

            // Interrupt the locks.
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);
                if (lock == null) continue;

                ((LockImpl)lock).destroy();
            }

            _lockTimeout.stopRunning();
            _lockHeartbeat.stopRunning();
            _lockUnlocked.stopRunning();

            destroy( _options );
        } catch (final Throwable t) { throw new DistributedLockException(t);
        } finally { _lock.unlock(); }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        _lock.lock();
        try {
            // only shutdown if not already cleaned up by user
            if ( _running.get() ) { shutdown(); }
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public boolean isRunning() { return _running.get(); }

    private Mongo _mongo;

    private final ReentrantLock _lock = new ReentrantLock(true);
    private final DistributedLockSvcOptions _options;

    private final AtomicBoolean _running = new AtomicBoolean(false);

    private Monitor.LockHeartbeat _lockHeartbeat;
    private Monitor.LockTimeout _lockTimeout;
    private Monitor.LockUnlocked _lockUnlocked;

    private final Map<String, DistributedLock> _locks = new ConcurrentHashMap<String, DistributedLock>();

    private static final Map<DistributedLockSvcOptions, DistributedLockSvc> _lockSvcs
            = new HashMap<DistributedLockSvcOptions, DistributedLockSvc>();

    private static void destroy(final DistributedLockSvcOptions pOptions) {
        if ( _lockSvcs.remove(pOptions) == null ) {
            throw new IllegalStateException( "lock service already unregistered for: " + pOptions );
        }
    }

    public static DistributedLockSvc create(final DistributedLockSvcOptions pOptions) {
        DistributedLockSvc svc = _lockSvcs.get(pOptions);
        if ( svc != null && svc.isRunning() ) { return svc; }

        synchronized(SvcImpl.class) {
            svc = _lockSvcs.get(pOptions);
            if ( svc != null && svc.isRunning() ) { return svc; }

            svc = new SvcImpl(pOptions);
            svc.startup();
            if ( _lockSvcs.put( pOptions, svc ) != null ) {
                throw new IllegalStateException( "lock service already registered for: " + pOptions );
            }

            return svc;
        }
    }

}

