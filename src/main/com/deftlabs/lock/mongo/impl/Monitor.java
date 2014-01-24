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
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;

// Mongo
import com.mongodb.Mongo;
import org.bson.types.ObjectId;

// Java
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The lock monitors.
 */
final class Monitor {

    /**
     * The lock heartbeat thread is responsible for sending updates to the lock
     * doc every X seconds (when the lock is owned by the current process). This
     * library uses missing/stale/old heartbeats to timeout locks that have not been
     * closed properly (based on the lock/unlock) contract. This can happen when processes
     * die unexpectedly (e.g., out of memory) or when they are not stopped properly (e.g., kill -9).
     */
    static class LockHeartbeat extends MonitorThread {

        LockHeartbeat(final Mongo pMongo,
                      final DistributedLockSvcOptions pSvcOptions,
                      final Map<String, DistributedLock> pLocks) {
            super("Mongo-Distributed-Lock-LockHeartbeat-" + System.currentTimeMillis(),
                    pMongo, pSvcOptions, pLocks);
        }

        @Override
        void monitor() {
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);

                final ObjectId lockId = lock.getLockId();

                if (!lock.isLocked() || lockId == null) continue;

                LockDao.heartbeat(_mongo, lockName, lockId, lock.getOptions(), _svcOptions);
            }
        }

        @Override
        long awaitMillis() {
            return _svcOptions.getHeartbeatFrequency();
        }
    }

    /**
     * The lock timeout thread impl (see LockHeartbeat docs for more info). One lock
     * timeout thread runs in each process this lock lib is running. This thread is
     * responsible for cleaning up expired locks (based on time since last heartbeat).
     */
    static class LockTimeout extends MonitorThread {

        LockTimeout(final Mongo pMongo, final DistributedLockSvcOptions pSvcOptions) {
            super("Mongo-Distributed-Lock-LockTimeout-" + System.currentTimeMillis(), pMongo, pSvcOptions);
        }

        @Override
        void monitor() {
            LockDao.expireInactiveLocks(_mongo, _svcOptions);
        }

        @Override
        long awaitMillis() {
            return _svcOptions.getTimeoutFrequency();
        }
    }

    /**
     * The lock unlocked thread is responsible for waking up local
     * threads when a lock state changes.
     */
    static class LockUnlocked extends MonitorThread {

        LockUnlocked(final Mongo pMongo,
                     final DistributedLockSvcOptions pSvcOptions,
                     final Map<String, DistributedLock> pLocks) {
            super("Mongo-Distributed-Lock-LockUnlocked-" + System.currentTimeMillis(),
                    pMongo, pSvcOptions, pLocks);
        }

        @Override
        void monitor() {
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);

                if (lock.isLocked()) continue;

                // Check to see if this is locked.
                if (LockDao.isLocked(_mongo, lockName, _svcOptions)) continue;

                // The lock is not locked, wakeup any blocking threads.
                lock.wakeupBlocked();
            }
        }

        @Override
        long awaitMillis() {
            return _svcOptions.getLockUnlockedFrequency();
        }
    }


    private static abstract class MonitorThread extends Thread {

        MonitorThread(final String pName,
                      final Mongo pMongo,
                      final DistributedLockSvcOptions pSvcOptions) {
            this(pName, pMongo, pSvcOptions, null);
        }

        MonitorThread(final String pName,
                      final Mongo pMongo,
                      final DistributedLockSvcOptions pSvcOptions,
                      final Map<String, DistributedLock> pLocks) {
            super(pName);
            _mongo = pMongo;
            _svcOptions = pSvcOptions;
            _locks = pLocks;
            _shutdown = new CountDownLatch(1);
            _exited = new CountDownLatch(1);
            setDaemon(true);
        }

        @Override public void run() {
            try {
                // do-while to eagerly try at startup for any initial cleanup.
                do {
                    try {
                        monitor();
                    } catch (final Throwable t) {
                        LOG.log(Level.SEVERE, t.getMessage(), t);
                    }
                } while (!_shutdown.await(awaitMillis(), TimeUnit.MILLISECONDS));
            } catch (final InterruptedException ignored) {
                // Safe exit.
                Thread.currentThread().interrupt();
            } finally {
                _exited.countDown();
            }
        }

        /**
         * Performs check and awaits shutdown signal for configured amount of milliseconds
         * @return true if shutdown() was called, false otherwise.
         */
        abstract void monitor();

        abstract long awaitMillis();

        void shutdown() throws InterruptedException {
            _shutdown.countDown();
            if (!_exited.await(10000, TimeUnit.MILLISECONDS)) {
                this.interrupt();
            }
        }

        final Mongo _mongo;
        final DistributedLockSvcOptions _svcOptions;
        final Map<String, DistributedLock> _locks;
        private final CountDownLatch _shutdown;
        private final CountDownLatch _exited;
    }

    private static final Logger LOG = Logger.getLogger("com.deftlabs.lock.mongo.Monitor");
}

