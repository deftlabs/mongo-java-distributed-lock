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
 * The distributed lock history dao. These are calls enabled by default, but can
 * be disabled in DistributedLockSvcOptions.
 */
final class LockHistoryDao extends BaseDao {

    /**
     * Insert an entry.
     */
    static void insert( final MongoClient pMongo,
                        final String pLockName,
                        final DistributedLockSvcOptions pSvcOptions,
                        final DistributedLockOptions pLockOptions,
                        long pServerTime,
                        final LockState pLockState,
                        final Object pLockId,
                        final boolean pTimedOut)
    {

        insert(pMongo, pLockName, pSvcOptions, pLockOptions.getInactiveLockTimeout(), pServerTime, pLockState, pLockId, pTimedOut);
    }


    /**
     * Insert an entry.
     */
    static void insert( final MongoClient pMongo,
                        final String pLockName,
                        final DistributedLockSvcOptions pSvcOptions,
                        final int pInactiveLockTimeout,
                        long pServerTime,
                        final LockState pLockState,
                        final Object pLockId,
                        final boolean pTimedOut)
    {
        final Thread currentThread = Thread.currentThread();

        long serverTime = pServerTime;

        if (serverTime == 0) serverTime = getServerTime(pMongo, pSvcOptions);

        final Date now = new Date(serverTime);

        final BasicDBObject lockDoc = new BasicDBObject(LockHistoryDef.LOCK_NAME.field, pLockName);
        lockDoc.put(LockHistoryDef.LIBRARY_VERSION.field, pSvcOptions.getLibVersion());
        lockDoc.put(LockHistoryDef.CREATED.field, now);
        lockDoc.put(LockHistoryDef.LOCK_ID.field, pLockId);
        lockDoc.put(LockHistoryDef.STATE.field, pLockState.code());
        lockDoc.put(LockHistoryDef.OWNER_APP_NAME.field, pSvcOptions.getAppName());
        lockDoc.put(LockHistoryDef.OWNER_ADDRESS.field, pSvcOptions.getHostAddress());
        lockDoc.put(LockHistoryDef.OWNER_HOSTNAME.field, pSvcOptions.getHostname());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_ID.field, currentThread.getId());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_NAME.field, currentThread.getName());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_GROUP_NAME.field, currentThread.getThreadGroup().getName());

        lockDoc.put(LockHistoryDef.INACTIVE_LOCK_TIMEOUT.field, pInactiveLockTimeout);

        lockDoc.put(LockHistoryDef.TIMED_OUT.field, pTimedOut);

        getDbCollection(pMongo, pSvcOptions).insert(lockDoc, WriteConcern.SAFE);
    }

    /**
     * Returns the history collection.
     */
    private static DBCollection getDbCollection(final MongoClient pMongo,
                                                final DistributedLockSvcOptions pSvcOptions)
    { return getDb(pMongo, pSvcOptions).getCollection(pSvcOptions.getHistoryCollectionName()); }

    /**
     * Ensure the proper indexes are on the collection. This must be called when
     * the service sarts.
     */
    static void setup(final MongoClient pMongo, final DistributedLockSvcOptions pSvcOptions) {

    }
}

