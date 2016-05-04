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
import com.deftlabs.lock.mongo.DistributedLockOptions;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;

// Mongo
import com.mongodb.*;
import org.bson.types.ObjectId;

// Java
import java.util.Date;

/**
 * The base dao.
 */
abstract class BaseDao {
    /**
     * Returns the db.
     */
    protected final static DB getDb(final MongoClient pMongo,
                                    final DistributedLockSvcOptions pSvcOptions)
    { return pMongo.getDB(pSvcOptions.getDbName()); }

    /**
     * Returns the current server time. This makes a few requests to the server to try and adjust for
     * network latency.
     */
    protected final static long getServerTime(  final MongoClient pMongo,
                                                final DistributedLockSvcOptions pSvcOptions)
    {

        final long [] localTimes = new long[SERVER_TIME_TRIES];
        final int [] latency = new int[SERVER_TIME_TRIES];

        long startTime;
        BasicDBObject serverStatus;

        for (int idx=0; idx < SERVER_TIME_TRIES; idx++) {
            startTime = System.currentTimeMillis();
            serverStatus = getDb(pMongo, pSvcOptions).command(_serverStatusCmd);
            latency[idx] = (int)(System.currentTimeMillis() - startTime);
            localTimes[idx] = ((Date)serverStatus.get(LOCAL_TIME_FIELD)).getTime();
        }

        final long serverTime = localTimes[(SERVER_TIME_TRIES -1)];

        // Adjust based on latency.
        return (serverTime + getHalfRoundedAvg(latency));
    }

    /**
     * We assume that latency is 50% each way.
     */
    protected final static int getHalfRoundedAvg(final int [] pV) {
        int total = 0;
        for (int idx=0; idx < pV.length; idx++) total += pV[idx];
        return Math.round((((float)total / (float)pV.length) / (float)2));
    }

    protected static final String INC = "$inc";
    protected static final String SET = "$set";

    protected static final String LT = "$lt";

    private static final String LOCAL_TIME_FIELD = "localTime";
    private static final int SERVER_TIME_TRIES = 3;

    protected static final int DUPLICATE_KEY_ERROR_CODE = 11000;

    protected static final BasicDBObject _serverStatusCmd = new BasicDBObject("serverStatus", 1);
}

