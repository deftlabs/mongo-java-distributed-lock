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
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

// JUnit
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

// Java
import java.util.concurrent.TimeUnit;

/**
 * Test the distributed lock. You must be running mongo on localhost:27017 for this
 * test to work.
 */
public final class DistributedLockIntTests {

    @Test
    public void testSimpleCreate() throws Exception {
        assertNotNull(createSimpleLockSvc());
    }

    @Test
    public void testSimpleLockCreateDestroy() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try { lock = lockSvc.create("testLock");
        } finally { if (lock != null) lockSvc.destroy(lock); }
    }

    @Test
    public void testSimpleLockCreateLockUnlockDestroy() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try { lock.lock();
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    @Test
    public void testSimpleLockWithNestedTryLock() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try {
                lock.lock();
                assertEquals(false, lock.tryLock());
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    @Test
    public void testSimpleTryLock() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try { lock.tryLock();
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    @Test
    public void testSimpleTimedTryLock() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try { assertEquals(true, lock.tryLock(0, TimeUnit.SECONDS));
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    @Test
    public void testSimpleTimedTryLock2() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try {
                lock.lock();
                assertEquals(false, lock.tryLock(0, TimeUnit.SECONDS));
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    @Test
    public void testSimpleTimedTryLock3() throws Exception {
        final DistributedLockSvc lockSvc = createSimpleLockSvc();
        DistributedLock lock = null;
        try {
            lock = lockSvc.create("testLock");
            try {
                lock.lock();

                final long startTime = System.currentTimeMillis();
                assertEquals(false, lock.tryLock(1, TimeUnit.SECONDS));

                final long blockTime = System.currentTimeMillis() - startTime;
                final long diff = blockTime - 1000;

                assertEquals(true, (diff <= 1));
            } finally { lock.unlock();  }
        } finally { if (lock != null) lockSvc.destroy(lock); }

        assertEquals(2, getHistoryCollection().count());
    }

    private DistributedLockSvc createSimpleLockSvc() {
        final DistributedLockSvcOptions options
        = new DistributedLockSvcOptions("mongodb://127.0.0.1:27017");

        final DistributedLockSvcFactory factory = new DistributedLockSvcFactory(options);
        return factory.getLockSvc();
    }

    @Before
    public void init() throws Exception {
        // Cleanup the test database
        getCollection().remove(new BasicDBObject());
        getHistoryCollection().remove(new BasicDBObject());
    }

    @After
    public void cleanup() {
        getCollection().remove(new BasicDBObject());
    }

    private DBCollection getCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("locks"); }

    private DBCollection getHistoryCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("lockHistory"); }

    public DistributedLockIntTests() throws Exception {
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
    }

    private final Mongo _mongo;

}

