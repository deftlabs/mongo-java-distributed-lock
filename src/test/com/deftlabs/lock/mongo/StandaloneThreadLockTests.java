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
 * The standalone threaded lock tests.
 */
public final class StandaloneThreadLockTests {

    private void test() {

    }

    private DBCollection getCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("locks"); }

    private DBCollection getHistoryCollection()
    { return _mongo.getDB("mongo-distributed-lock").getCollection("lockHistory"); }

    private StandaloneThreadLockTests() throws Exception {
        _mongo = new Mongo(new MongoURI("mongodb://127.0.0.1:27017"));
    }

    public static void main(final String [] pArgs) throws Exception {
        final StandaloneThreadLockTests tests = new StandaloneThreadLockTests();
        tests.test();

    }

    private final Mongo _mongo;

}

