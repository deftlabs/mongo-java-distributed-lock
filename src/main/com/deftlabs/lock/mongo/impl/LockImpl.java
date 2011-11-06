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
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;

// Mongo
import com.mongodb.Mongo;
import org.bson.types.ObjectId;

// Java
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * The distributed lock object.
 */
public class LockImpl implements DistributedLock {

    /**
     * Construct the object with params.
     */
    LockImpl(   final Mongo pMongo,
                final String pName,
                final DistributedLockOptions pLockOptions,
                final DistributedLockSvcOptions pSvcOptions)
    {
        _mongo = pMongo;
        _name = pName;
        _lockOptions = pLockOptions;
        _svcOptions = pSvcOptions;
    }

    @Override
    public void lock() {
        if (tryDistributedLock()) return;
        park();
    }

    /**
     * Park the current thread. This method will check to see (when allowed) to see
     * if it can get the distributed lock.
     */
    private void park() {

        boolean wasInterrupted = false;
        Thread current = Thread.currentThread();
        _waitingThreads.add(current);

        // Block while not first in queue or cannot acquire lock
        while (_running.get()) {

            if (Thread.interrupted()) { wasInterrupted = true; break; }

            if (_waitingThreads.peek() == current && !isLocked()) {
                // Check to see if this thread can get the distributed lock
                if (tryDistributedLock()) break;
            }

            LockSupport.park(this);
        }

        _waitingThreads.remove();
        if (wasInterrupted) current.interrupt();
    }

    /**
     * Try and lock the distributed lock.
     */
    private boolean tryDistributedLock() {
        if (isLocked()) return false;

        final ObjectId lockId = LockDao.lock(_mongo, _name, _svcOptions, _lockOptions);

        if (lockId == null) return false;

        _locked.set(true);
        _lockId = lockId;
        return true;
    }

    /**
     * This is not supported.
     */
    @Override
    public void lockInterruptibly()
    { throw new UnsupportedOperationException("not implemented"); }

    /**
     * For now, this is not supported.
     */
    @Override
    public Condition newCondition()
    { throw new UnsupportedOperationException("not implemented"); }

    /**
     * Does not block. Returns right away if not able to lock.
     */
    @Override
    public boolean tryLock() {
        if (isLocked()) return false;

        final ObjectId lockId = LockDao.lock(_mongo, _name, _svcOptions, _lockOptions);

        if (lockId == null) return false;

        _locked.set(true);
        _lockId = lockId;
        return true;
    }

    @Override
    public boolean tryLock(final long pTime, final TimeUnit pTimeUnit) {
        if (!isLocked()) {
            boolean locked = tryLock();
            if (locked) return true;
        }

        // Try and lock every X amount of time for pTime to get the lock.

        return false;
    }

    @Override
    public void unlock() {
        LockDao.unlock(_mongo, _name, _svcOptions, _lockOptions, _lockId);
        _locked.set(false);
        _lockId = null;
        LockSupport.unpark(_waitingThreads.peek());
    }

    /**
     * Called to initialize the lock.
     */
    void init() {
        _running.set(true);
        // Start the heartbeat thread
        // Start the lock monitor thread

        // The monitor thread needs to see if the lock has been released by another process
        // and let the local thread(s) try to get. If they are not able to acquire, then they
        // will go back into park mode.
    }

    /**
     * Called to destroy the lock.
     */
    void destroy() {
        _running.set(false);
        for (final Thread t : _waitingThreads) t.interrupt();
    }

    /**
     * Returns true if the lock is currently locked.
     */
    @Override
    public boolean isLocked() { return _locked.get(); }

    /**
     * Returns the lock name.
     */
    @Override
    public String getName() { return _name; }

    private final String _name;
    private final Mongo _mongo;
    private final DistributedLockOptions _lockOptions;
    private final DistributedLockSvcOptions _svcOptions;

    private volatile ObjectId _lockId;

    private final AtomicBoolean _locked = new AtomicBoolean(false);

    private final AtomicBoolean _running = new AtomicBoolean(false);

    private final Queue<Thread> _waitingThreads = new ConcurrentLinkedQueue<Thread>();

}

