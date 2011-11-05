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
import org.junit.BeforeClass;
import static org.junit.Assert.*;

// Java
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

/**
 * Test the distributed lock.
 */
public final class DistributedLockIntTests {

    @Test
    public void testSimpleWarning() throws Exception {

    }

    @BeforeClass
    public static void setupLogger() throws Exception {

    }

    @Before
    public void init() throws Exception {
        // Cleanup the test database
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
        getCollection().remove(new BasicDBObject());
    }

    @After
    public void cleanup() { getCollection().remove(new BasicDBObject()); }

    private DBCollection getCollection()
    { return _mongo.getDB("mongo-java-distributed-lock").getCollection("locks"); }

    private Mongo _mongo;

    private static final Logger LOG = Logger.getLogger(DistributedLockIntTests.class.getName());
}

