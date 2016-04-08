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

import com.mongodb.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

// JUnit
// Java

/**
 * Test assumptions about the java driver for mongo.
 */
public final class DriverIntTest {

    @Test public void testWriteConcerns() throws Exception {
        final BasicDBObject doc = new BasicDBObject("_id", "test");

        getCollection().insert(doc, WriteConcern.UNACKNOWLEDGED);

        assertThatExceptionOfType(DuplicateKeyException.class)
                .isThrownBy(() -> getCollection().insert(doc, WriteConcern.ACKNOWLEDGED));
    }

    @Test public void testFindAndModify() throws Exception {

        final BasicDBObject query = new BasicDBObject("_id", "test");

        final BasicDBObject toSet = new BasicDBObject("locked", true);

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("_id", 1), null, false, new BasicDBObject("$set", toSet), false, false);

        assertThat(lockDoc).isNull();
    }

    @Test public void testFindAndModifyWithDoc() throws Exception {
        final BasicDBObject insert = new BasicDBObject("_id", "test");
        insert.put("locked", false);
        insert.put("lockId", "10");
        getCollection().insert(insert, WriteConcern.ACKNOWLEDGED);


        final BasicDBObject query = new BasicDBObject("_id", "test");
        query.put("locked", false);
        query.put("lockId", "10");

        final BasicDBObject toSet = new BasicDBObject("locked", true);
        toSet.put("lockId", "20");

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("lockId", 1), null, false, new BasicDBObject("$set", toSet), true, false);

        assertThat(lockDoc).isNotNull();
        assertThat(lockDoc.getString("lockId")).isEqualTo("20");
    }

    @Test public void testFindAndModifyWithDocMiss() throws Exception {
        final BasicDBObject insert = new BasicDBObject("_id", "test");
        insert.put("locked", false);
        insert.put("lockId", "10");
        getCollection().insert(insert, WriteConcern.ACKNOWLEDGED);

        final BasicDBObject query = new BasicDBObject("_id", "test");
        query.put("locked", false);
        query.put("lockId", "20");

        final BasicDBObject toSet = new BasicDBObject("locked", true);
        toSet.put("lockId", "20");

        final BasicDBObject lockDoc
        = (BasicDBObject)getCollection().findAndModify(query, new BasicDBObject("lockId", 1), null, false, new BasicDBObject("$set", toSet), true, false);

        assertThat(lockDoc).isNull();

    }

    @Before public void init() throws Exception { getDb().dropDatabase(); }

    @After public void cleanup() { getDb().dropDatabase(); }

    private DBCollection getCollection() { return getDb().getCollection("test"); }

    private DB getDb() { return _mongo.getDB("mongo-distributed-lock-test"); }

    public DriverIntTest() throws Exception {
        _mongo = new MongoClient(new MongoClientURI("mongodb://127.0.0.1:27017"));
    }

    private final MongoClient _mongo;

}

