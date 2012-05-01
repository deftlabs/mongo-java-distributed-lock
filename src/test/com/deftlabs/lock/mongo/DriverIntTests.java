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

    @Before public void init() throws Exception { getDb().dropDatabase(); }

    //@After public void cleanup() { getDb().dropDatabase(); }

    private DBCollection getCollection() { return getDb().getCollection("test"); }

    private DB getDb() { return _mongo.getDB("mongo-distributed-lock-test"); }

    public DriverIntTests() throws Exception {
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
    }

    private final Mongo _mongo;

}

