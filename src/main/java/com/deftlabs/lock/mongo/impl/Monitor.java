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
import com.mongodb.MongoClient;
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

        LockHeartbeat(final MongoClient pMongo,
                      final DistributedLockSvcOptions pSvcOptions,
                      final Map<String, DistributedLock> pLocks) {
            super("Mongo-Distributed-Lock-LockHeartbeat-" + System.currentTimeMillis(),
                    pMongo, pSvcOptions, pLocks);
        }

        @Override
        boolean monitor() throws InterruptedException {
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);

                final ObjectId lockId = lock.getLockId();

                if (!lock.isLocked() || lockId == null) continue;

                LockDao.heartbeat(_mongo, lockName, lockId, lock.getOptions(), _svcOptions);
            }

            return _shutdown.await(_svcOptions.getHeartbeatFrequency(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The lock timeout thread impl (see LockHeartbeat docs for more info). One lock
     * timeout thread runs in each process this lock lib is running. This thread is
     * responsible for cleaning up expired locks (based on time since last heartbeat).
     */
    static class LockTimeout extends MonitorThread {

        LockTimeout(final MongoClient pMongo, final DistributedLockSvcOptions pSvcOptions) {
            super("Mongo-Distributed-Lock-LockTimeout-" + System.currentTimeMillis(), pMongo, pSvcOptions);
        }

        @Override
        boolean monitor() throws InterruptedException {
            LockDao.expireInactiveLocks(_mongo, _svcOptions);
            return _shutdown.await(_svcOptions.getTimeoutFrequency(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * The lock unlocked thread is responsible for waking up local
     * threads when a lock state changes.
     */
    static class LockUnlocked extends MonitorThread {

        LockUnlocked(final MongoClient pMongo,
                     final DistributedLockSvcOptions pSvcOptions,
                     final Map<String, DistributedLock> pLocks) {
            super("Mongo-Distributed-Lock-LockUnlocked-" + System.currentTimeMillis(),
                    pMongo, pSvcOptions, pLocks);
        }

        @Override
        boolean monitor() throws InterruptedException {
            for (final String lockName : _locks.keySet()) {
                final DistributedLock lock = _locks.get(lockName);

                if (lock.isLocked()) continue;

                // Check to see if this is locked.
                if (LockDao.isLocked(_mongo, lockName, _svcOptions)) continue;

                // The lock is not locked, wakeup any blocking threads.
                lock.wakeupBlocked();
            }

            return _shutdown.await(_svcOptions.getLockUnlockedFrequency(), TimeUnit.MILLISECONDS);
        }
    }


    private static abstract class MonitorThread extends Thread {

        MonitorThread(final String pName,
                      final MongoClient pMongo,
                      final DistributedLockSvcOptions pSvcOptions) {
            this(pName, pMongo, pSvcOptions, null);
        }

        MonitorThread(final String pName,
                      final MongoClient pMongo,
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
            boolean shutdown = false;
            try {
                while (!shutdown) {
                    try { shutdown = monitor();
                    } catch (final InterruptedException ie) { break;
                    } catch (final Throwable t) { LOG.log(Level.SEVERE, t.getMessage(), t); }
                }
            } finally {
                _exited.countDown();
            }
        }

        /**
         * Performs check and awaits shutdown signal for configured amount of milliseconds
         * @return true if shutdown() was called, false otherwise.
         */
        abstract boolean monitor() throws InterruptedException;

        void shutdown() throws InterruptedException {
            _shutdown.countDown();
            if (!_exited.await(10000, TimeUnit.MILLISECONDS)) {
                this.interrupt();
            }
        }

        final MongoClient _mongo;
        final DistributedLockSvcOptions _svcOptions;
        final Map<String, DistributedLock> _locks;
        final CountDownLatch _shutdown;
        final CountDownLatch _exited;
    }

    private static final Logger LOG = Logger.getLogger("com.deftlabs.lock.mongo.Monitor");
}

