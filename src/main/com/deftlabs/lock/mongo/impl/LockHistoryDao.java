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
import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;
import com.mongodb.WriteConcern;
import com.mongodb.CommandResult;
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
    static void insert( final Mongo pMongo,
                        final String pLockName,
                        final DistributedLockSvcOptions pSvcOptions,
                        final DistributedLockOptions pLockOptions,
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
        lockDoc.put(LockHistoryDef.UPDATED.field, now);
        lockDoc.put(LockHistoryDef.LAST_HEARTBEAT.field, now);
        lockDoc.put(LockHistoryDef.LOCK_ACQUIRED_TIME.field, now);
        lockDoc.put(LockHistoryDef.LOCK_ID.field, pLockId);
        lockDoc.put(LockHistoryDef.STATE.field, pLockState.code());
        lockDoc.put(LockHistoryDef.OWNER_APP_NAME.field, pSvcOptions.getAppName());
        lockDoc.put(LockHistoryDef.OWNER_ADDRESS.field, pSvcOptions.getHostAddress());
        lockDoc.put(LockHistoryDef.OWNER_HOSTNAME.field, pSvcOptions.getHostname());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_ID.field, currentThread.getId());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_NAME.field, currentThread.getName());
        lockDoc.put(LockHistoryDef.OWNER_THREAD_GROUP_NAME.field, currentThread.getThreadGroup().getName());

        // TODO: Support tracking this
        //lockDoc.put(LockDef.LOCK_ATTEMPT_COUNT.field, 0);

        lockDoc.put(LockHistoryDef.INACTIVE_LOCK_TIMEOUT.field, pLockOptions.getInactiveLockTimeout());

        lockDoc.put(LockHistoryDef.TIMED_OUT.field, pTimedOut);

        getDbCollection(pMongo, pSvcOptions).insert(lockDoc);
    }

    /**
     * This will try and create the object. If successful, it will return the lock id.
     * Otherwise, it will return null (i.e., no lock).
     */
    static void insert(   final Mongo pMongo,
                                            final String pLockName,
                                            final DistributedLockSvcOptions pSvcOptions,
                                            final DistributedLockOptions pLockOptions,
                                            final long pServerTime,
                                            final long pStartTime)
    {
        final long adjustTime = System.currentTimeMillis() - pStartTime;
        final Date now = new Date((pServerTime + adjustTime));
        final ObjectId lockId = ObjectId.get();

        final BasicDBObject lockDoc = new BasicDBObject(LockDef.ID.field, pLockName);
        lockDoc.put(LockDef.LIBRARY_VERSION.field, pSvcOptions.getLibVersion());
        lockDoc.put(LockDef.UPDATED.field, now);
        lockDoc.put(LockDef.LAST_HEARTBEAT.field, now);
        lockDoc.put(LockDef.LOCK_ACQUIRED_TIME.field, now);
        lockDoc.put(LockDef.LOCK_ID.field, lockId);
        lockDoc.put(LockDef.STATE.field, LockState.LOCKED.code());
        lockDoc.put(LockDef.OWNER_APP_NAME.field, pSvcOptions.getAppName());
        lockDoc.put(LockDef.OWNER_ADDRESS.field, pSvcOptions.getHostAddress());
        lockDoc.put(LockDef.OWNER_HOSTNAME.field, pSvcOptions.getHostname());
        lockDoc.put(LockDef.OWNER_THREAD_ID.field, Thread.currentThread().getId());
        lockDoc.put(LockDef.OWNER_THREAD_NAME.field, Thread.currentThread().getName());
        lockDoc.put(LockDef.OWNER_THREAD_GROUP_NAME.field, Thread.currentThread().getThreadGroup().getName());
        lockDoc.put(LockDef.LOCK_ATTEMPT_COUNT.field, 0);
        lockDoc.put(LockDef.INACTIVE_LOCK_TIMEOUT.field, pLockOptions.getInactiveLockTimeout());

        // Insert, if successful then get out of here.
        final WriteResult result = getDbCollection(pMongo, pSvcOptions).insert(lockDoc, WriteConcern.NORMAL);
        final CommandResult cmdResult = result.getLastError();

    }

    /**
     * Returns the history collection.
     */
    private static DBCollection getDbCollection(final Mongo pMongo,
                                                final DistributedLockSvcOptions pSvcOptions)
    { return getDb(pMongo, pSvcOptions).getCollection(pSvcOptions.getHistoryCollectionName()); }

    /**
     * Ensure the proper indexes are on the collection. This must be called when
     * the service sarts.
     */
    static void setup(final Mongo pMongo, final DistributedLockSvcOptions pSvcOptions) {

    }
}

