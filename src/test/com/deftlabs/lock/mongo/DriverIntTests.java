/**
 * Copyright 2012, Deft Labs.
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
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.CommandResult;

// JUnit
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

// Java
import java.util.concurrent.TimeUnit;

/**
 * Test assumptions about the java driver for mongo.
 */
public final class DriverIntTests {

    @Test public void testWriteConcerns() throws Exception {
        final BasicDBObject doc = new BasicDBObject("_id", "test");

        getDb().requestStart();
        getCollection().insert(doc, WriteConcern.NORMAL);

        final WriteResult result = getCollection().insert(doc, WriteConcern.NORMAL);
        final CommandResult cmdResult = result.getLastError(WriteConcern.NORMAL);

        assertTrue(cmdResult.getException() != null);

        //System.out.println(cmdResult);

        getDb().requestDone();
    }

    @Test public void testFindAndModify() throws Exception {

        final BasicDBObject query = new BasicDBObject("_id", "test");

        final BasicDBObject toSet = new BasicDBObject("locked", true);

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("_id", 1), null, false, new BasicDBObject("$set", toSet), false, false);

        assertNull(lockDoc);
    }

    @Test public void testFindAndModifyWithDoc() throws Exception {
        final BasicDBObject insert = new BasicDBObject("_id", "test");
        insert.put("locked", false);
        insert.put("lockId", "10");
        getCollection().insert(insert, WriteConcern.SAFE);


        final BasicDBObject query = new BasicDBObject("_id", "test");
        query.put("locked", false);
        query.put("lockId", "10");

        final BasicDBObject toSet = new BasicDBObject("locked", true);
        toSet.put("lockId", "20");

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("lockId", 1), null, false, new BasicDBObject("$set", toSet), true, false);

        assertNotNull(lockDoc);

        assertEquals("20", lockDoc.getString("lockId"));
    }

    @Test public void testFindAndModifyWithDocMiss() throws Exception {
        final BasicDBObject insert = new BasicDBObject("_id", "test");
        insert.put("locked", false);
        insert.put("lockId", "10");
        getCollection().insert(insert, WriteConcern.SAFE);

        final BasicDBObject query = new BasicDBObject("_id", "test");
        query.put("locked", false);
        query.put("lockId", "20");

        final BasicDBObject toSet = new BasicDBObject("locked", true);
        toSet.put("lockId", "20");

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("lockId", 1), null, false, new BasicDBObject("$set", toSet), true, false);

        assertNull(lockDoc);

    }

    @Before public void init() throws Exception { getDb().dropDatabase(); }

    @After public void cleanup() { getDb().dropDatabase(); }

    private DBCollection getCollection() { return getDb().getCollection("test"); }

    private DB getDb() { return _mongo.getDB("mongo-distributed-lock-test"); }

    public DriverIntTests() throws Exception {
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
    }

    private final Mongo _mongo;

}

