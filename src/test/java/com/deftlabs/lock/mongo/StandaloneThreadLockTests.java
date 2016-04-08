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

package com.deftlabs.lock.mongo;

// Mongo

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

// JUnit
// Java


/**
 * The standalone threaded lock tests.
 */
public final class StandaloneThreadLockTests {

    private void test() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(THREAD_COUNT);

        final AtomicBoolean locked = new AtomicBoolean(false);
        final DistributedLockSvc lockSvc = createSimpleLockSvc();

        try {

            for (int idx=0; idx < THREAD_COUNT; idx++)
            { new Thread(new LockTest(countDownLatch, lockSvc, locked)).start(); }

            countDownLatch.await();

        } finally { lockSvc.shutdown(); }
    }

    private DistributedLockSvc createSimpleLockSvc() {
        final DistributedLockSvcOptions options
        = new DistributedLockSvcOptions("mongodb://127.0.0.1:27017");

        options.setEnableHistory(false);

        final DistributedLockSvcFactory factory = new DistributedLockSvcFactory(options);
        return factory.getLockSvc();
    }

    private static class LockTest implements Runnable {

        private LockTest(   final CountDownLatch pCountDownLatch,
                            final DistributedLockSvc pLockSvc,
                            final AtomicBoolean pLocked)
        {
            _countDownLatch = pCountDownLatch;
            _lockSvc = pLockSvc;
            _locked = pLocked;
        }

        @Override public void run() {

            final DistributedLock lock = _lockSvc.create("com.deftlabs.lock.mongo.testLock");

            final long startTime = System.currentTimeMillis();

            for (int idx = 0; idx < LOCK_TEST_COUNT; idx++) {

                try {
                    lock.lock();

                    if (!_locked.compareAndSet(false, true)) throw new IllegalStateException("Already locked");

                } finally {
                    if (!_locked.compareAndSet(true, false)) throw new IllegalStateException("Already unlocked");
                    lock.unlock();
                }
            }

            final long execTime = System.currentTimeMillis() - startTime;
            
            _countDownLatch.countDown();
        }

        private final DistributedLockSvc _lockSvc;
        private final CountDownLatch _countDownLatch;
        private final AtomicBoolean _locked;
    }

    private DBCollection getCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("locks"); }

    private DBCollection getHistoryCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("lockHistory"); }

    private StandaloneThreadLockTests() throws Exception
    { _mongo = new MongoClient(new MongoClientURI("mongodb://127.0.0.1:27017")); }

    private static final int THREAD_COUNT = 200;

    private static final int LOCK_TEST_COUNT = 50;

    public static void main(final String [] pArgs) throws Exception {
        final StandaloneThreadLockTests tests = new StandaloneThreadLockTests();
        tests.test();
    }

    private final MongoClient _mongo;

}

