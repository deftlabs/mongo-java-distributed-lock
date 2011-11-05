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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

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

        if (!isLocked()) {
            final ObjectId lockId = LockDao.lock(_mongo, _name, _svcOptions, _lockOptions);

            if (lockId != null) {
                _lockId = lockId;
                _locked.set(true);
                return;
            }
        }

        // The lock is not available - block using LockSupport
    }

    @Override
    public void lockInterruptibly() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }

    /**
     * Does not block. Returns right away if not able to lock.
     */
    @Override
    public boolean tryLock() {
        if (isLocked()) return false;

        final ObjectId lockId = LockDao.lock(_mongo, _name, _svcOptions, _lockOptions);

        if (lockId != null) {
            _lockId = lockId;
            _locked.set(true);
            return true;
        }

        return false;
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
        _locked.set(false);
        _lockId = null;
        LockDao.unlock(_mongo, _name, _svcOptions, _lockOptions, _lockId);
    }

    /**
     * Called to inialize the lock.
     */
    void init() {
        // Start the heartbeat thread
        // Start the lock monitor thread
    }

    /**
     * Called to destroy the lock.
     */
    void destroy() {

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

    private final ReentrantLock _localLock = new ReentrantLock(true);
}

