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
import com.mongodb.DBCursor;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;
import com.mongodb.WriteConcern;
import com.mongodb.CommandResult;
import org.bson.types.ObjectId;

// Java
import java.util.Date;

/**
 * The distributed lock dao. These are a set of static methods
 * that are responsible for data access.
 */
final class LockDao extends BaseDao {

    /**
     * Increment the lock heartbeat time.
     */
    static void heartbeat(  final Mongo pMongo,
                            final String pLockName,
                            final ObjectId pLockId,
                            final DistributedLockOptions pLockOptions,
                            final DistributedLockSvcOptions pSvcOptions)

    {
        try {
            requestStart(pMongo, pSvcOptions);

            final long serverTime = getServerTime(pMongo, pSvcOptions);

            final BasicDBObject query = new BasicDBObject(LockDef.ID.field, pLockName);
            query.put(LockDef.LOCK_ID.field, pLockId);
            query.put(LockDef.STATE.field, LockState.LOCKED.code());

            final BasicDBObject toSet = new BasicDBObject(LockDef.LAST_HEARTBEAT.field, new Date(serverTime));
            toSet.put(LockDef.LOCK_TIMEOUT_TIME.field, new Date(serverTime + pLockOptions.getInactiveLockTimeout()));

            getDbCollection(pMongo, pSvcOptions).update(query, new BasicDBObject(SET, toSet), false, false);

        } finally { requestDone(pMongo, pSvcOptions); }
    }

    /**
     * Try and get the lock. If unable to do so, this returns false.
     */
    static synchronized ObjectId lock(  final Mongo pMongo,
                                        final String pLockName,
                                        final DistributedLockSvcOptions pSvcOptions,
                                        final DistributedLockOptions pLockOptions)
    {
        try {
            requestStart(pMongo, pSvcOptions);

            // Lookup the lock object.
            BasicDBObject lockDoc = findById(pMongo, pLockName, pSvcOptions);

            final long serverTime = getServerTime(pMongo, pSvcOptions);
            final long startTime = System.currentTimeMillis();

            // The doc was not there so we are going to try and insert a new doc.
            if (lockDoc == null) {
                final ObjectId lockId
                = tryInsertNew(pMongo, pLockName, pSvcOptions, pLockOptions,serverTime, startTime);
                if (lockId != null) return lockId;
            }

            // Get the state.
            final LockState lockState = LockState.findByCode(lockDoc.getString(LockDef.STATE.field));

            // If it is unlocked, then try and lock.
            if (lockState.isUnlocked()) {
                final ObjectId lockId
                = tryLockingExisting( pMongo, pLockName, pSvcOptions, pLockOptions, serverTime, startTime);
                if (lockId != null) return lockId;
            }

            // Could not get the lock.
            incrementLockAttemptCount(pMongo, pLockName, pSvcOptions);

            return null;
        } finally { requestDone(pMongo, pSvcOptions); }
    }

    private static ObjectId tryLockingExisting( final Mongo pMongo,
                                                final String pLockName,
                                                final DistributedLockSvcOptions pSvcOptions,
                                                final DistributedLockOptions pLockOptions,
                                                final long pServerTime,
                                                final long pStartTime)
    {
        final long adjustTime = System.currentTimeMillis() - pStartTime;

        final long serverTime = pServerTime + adjustTime;
        final Date now = new Date(serverTime);

        final ObjectId lockId = ObjectId.get();

        final BasicDBObject query = new BasicDBObject(LockDef.ID.field, pLockName);
        query.put(LockDef.STATE.field, LockState.UNLOCKED.code());

        final BasicDBObject toSet = new BasicDBObject();
        toSet.put(LockDef.LIBRARY_VERSION.field, pSvcOptions.getLibVersion());
        toSet.put(LockDef.UPDATED.field, now);
        toSet.put(LockDef.LAST_HEARTBEAT.field, now);
        toSet.put(LockDef.LOCK_ACQUIRED_TIME.field, now);
        toSet.put(LockDef.LOCK_TIMEOUT_TIME.field, new Date((serverTime + pLockOptions.getInactiveLockTimeout())));
        toSet.put(LockDef.LOCK_ID.field, lockId);
        toSet.put(LockDef.STATE.field, LockState.LOCKED.code());
        toSet.put(LockDef.OWNER_APP_NAME.field, pSvcOptions.getAppName());
        toSet.put(LockDef.OWNER_ADDRESS.field, pSvcOptions.getHostAddress());
        toSet.put(LockDef.OWNER_HOSTNAME.field, pSvcOptions.getHostname());
        toSet.put(LockDef.OWNER_THREAD_ID.field, Thread.currentThread().getId());
        toSet.put(LockDef.OWNER_THREAD_NAME.field, Thread.currentThread().getName());
        toSet.put(LockDef.OWNER_THREAD_GROUP_NAME.field, Thread.currentThread().getThreadGroup().getName());
        toSet.put(LockDef.LOCK_ATTEMPT_COUNT.field, 0);
        toSet.put(LockDef.INACTIVE_LOCK_TIMEOUT.field, pLockOptions.getInactiveLockTimeout());

        final BasicDBObject lockDoc
        = (BasicDBObject)getDbCollection(pMongo, pSvcOptions).findAndModify(query, new BasicDBObject(LockDef.ID.field, 1), null, false, new BasicDBObject(SET, toSet), false, false);

        if (lockDoc != null && lockDoc.containsField(LockDef.ID.field)) {

            if (pSvcOptions.getEnableHistory())
            { LockHistoryDao.insert( pMongo, pLockName, pSvcOptions, pLockOptions, serverTime, LockState.LOCKED, lockId, false); }

            return lockId;
        }

        // Someone else beat us to the punch.
        return null;
    }

    /**
     * This will try and create the object. If successful, it will return the lock id.
     * Otherwise, it will return null (i.e., no lock).
     */
    private static ObjectId tryInsertNew(   final Mongo pMongo,
                                            final String pLockName,
                                            final DistributedLockSvcOptions pSvcOptions,
                                            final DistributedLockOptions pLockOptions,
                                            final long pServerTime,
                                            final long pStartTime)
    {
        final long adjustTime = System.currentTimeMillis() - pStartTime;

        final long serverTime = pServerTime + adjustTime;
        final Date now = new Date(serverTime);
        final ObjectId lockId = ObjectId.get();

        final Thread currentThread = Thread.currentThread();

        final BasicDBObject lockDoc = new BasicDBObject(LockDef.ID.field, pLockName);
        lockDoc.put(LockDef.LIBRARY_VERSION.field, pSvcOptions.getLibVersion());
        lockDoc.put(LockDef.UPDATED.field, now);
        lockDoc.put(LockDef.LAST_HEARTBEAT.field, now);
        lockDoc.put(LockDef.LOCK_ACQUIRED_TIME.field, now);
        lockDoc.put(LockDef.LOCK_ID.field, lockId);
        lockDoc.put(LockDef.STATE.field, LockState.LOCKED.code());
        lockDoc.put(LockDef.LOCK_TIMEOUT_TIME.field, new Date((serverTime + pLockOptions.getInactiveLockTimeout())));
        lockDoc.put(LockDef.OWNER_APP_NAME.field, pSvcOptions.getAppName());
        lockDoc.put(LockDef.OWNER_ADDRESS.field, pSvcOptions.getHostAddress());
        lockDoc.put(LockDef.OWNER_HOSTNAME.field, pSvcOptions.getHostname());
        lockDoc.put(LockDef.OWNER_THREAD_ID.field, currentThread.getId());
        lockDoc.put(LockDef.OWNER_THREAD_NAME.field, currentThread.getName());
        lockDoc.put(LockDef.OWNER_THREAD_GROUP_NAME.field, currentThread.getThreadGroup().getName());
        lockDoc.put(LockDef.LOCK_ATTEMPT_COUNT.field, 0);
        lockDoc.put(LockDef.INACTIVE_LOCK_TIMEOUT.field, pLockOptions.getInactiveLockTimeout());

        // Insert, if successful then get out of here.
        final WriteResult result = getDbCollection(pMongo, pSvcOptions).insert(lockDoc, WriteConcern.NORMAL);
        final CommandResult cmdResult = result.getLastError();

        if (cmdResult.getErrorMessage() != null) return null;

       if (pSvcOptions.getEnableHistory())
       { LockHistoryDao.insert( pMongo, pLockName, pSvcOptions, pLockOptions, serverTime, LockState.LOCKED, lockId, false); }

        return lockId;
    }

    /**
     * Find by lock name/id.
     */
    static BasicDBObject findById(  final Mongo pMongo,
                                    final String pLockName,
                                    final DistributedLockSvcOptions pSvcOptions)
    { return (BasicDBObject)getDbCollection(pMongo, pSvcOptions).findOne(new BasicDBObject(LockDef.ID.field, pLockName)); }

    /**
     * Increment the waiting request count. This can be used by application developers
     * to diagnose problems with their applications.
     */
    static void incrementLockAttemptCount(  final Mongo pMongo,
                                            final String pLockName,
                                            final DistributedLockSvcOptions pSvcOptions)
    {
        getDbCollection(pMongo, pSvcOptions)
        .update(new BasicDBObject(LockDef.ID.field, pLockName), new BasicDBObject(INC, new BasicDBObject(LockDef.LOCK_ATTEMPT_COUNT.field, 1)), false, false);
    }

    /**
     * Unlock the lock.
     */
    static synchronized void unlock(final Mongo pMongo,
                                    final String pLockName,
                                    final DistributedLockSvcOptions pSvcOptions,
                                    final DistributedLockOptions pLockOptions,
                                    final ObjectId pLockId)
    {
        final BasicDBObject toSet = new BasicDBObject();
        toSet.put(LockDef.LIBRARY_VERSION.field, null);
        toSet.put(LockDef.UPDATED.field, new Date(getServerTime(pMongo, pSvcOptions)));
        toSet.put(LockDef.LOCK_ACQUIRED_TIME.field, null);
        toSet.put(LockDef.LOCK_TIMEOUT_TIME.field, null);
        toSet.put(LockDef.LOCK_ID.field, null);
        toSet.put(LockDef.STATE.field, LockState.UNLOCKED.code());
        toSet.put(LockDef.OWNER_APP_NAME.field, null);
        toSet.put(LockDef.OWNER_ADDRESS.field, null);
        toSet.put(LockDef.OWNER_HOSTNAME.field, null);
        toSet.put(LockDef.OWNER_THREAD_ID.field, null);
        toSet.put(LockDef.OWNER_THREAD_NAME.field, null);
        toSet.put(LockDef.OWNER_THREAD_GROUP_NAME.field, null);
        toSet.put(LockDef.LOCK_ATTEMPT_COUNT.field, 0);
        toSet.put(LockDef.INACTIVE_LOCK_TIMEOUT.field, pLockOptions.getInactiveLockTimeout());

        final BasicDBObject query = new BasicDBObject(LockDef.ID.field, pLockName);
        query.put(LockDef.LOCK_ID.field, pLockId);
        query.put(LockDef.STATE.field, LockState.LOCKED.code());

        final BasicDBObject lockDoc
        = (BasicDBObject)getDbCollection(pMongo, pSvcOptions).findAndModify(query, null, null, false, new BasicDBObject(SET, toSet), false, false);

        LockHistoryDao.insert(pMongo, pLockName, pSvcOptions, pLockOptions, 0, LockState.UNLOCKED, pLockId, false);
    }

    /**
     * Check for expired/inactive/dead locks and unlock.
     */
    static void expireInactiveLocks(final Mongo pMongo,
                                    final DistributedLockSvcOptions pSvcOptions)
    {

        // Adjust the time buffer to  make sure we don't have small time issues.
        final long queryServerTime = getServerTime(pMongo, pSvcOptions);

        final BasicDBObject query = new BasicDBObject(LockDef.STATE.field, LockState.LOCKED.code());
        query.put(LockDef.LOCK_TIMEOUT_TIME.field, new BasicDBObject(LT, new Date(queryServerTime)));

        final DBCursor cur = getDbCollection(pMongo, pSvcOptions).find(query).batchSize(10);

        try {
            while (cur.hasNext()) {
                // We are going to double check and timeout if necessary.
                // Look at the last ping date and expire timeout... make sure we
                // don't step on a short lived lock (with fast fail).
                final long serverTime = getServerTime(pMongo, pSvcOptions);

                // TODO: Add the findAndModify to timeout the lock.

            }
        } finally { if (cur != null) cur.close(); }
    }

    /**
     * Returns the collection.
     */
    private static DBCollection getDbCollection(final Mongo pMongo,
                                                final DistributedLockSvcOptions pSvcOptions)
    { return getDb(pMongo, pSvcOptions).getCollection(pSvcOptions.getCollectionName()); }

    /**
     * Ensure the proper indexes are on the collection. This must be called when
     * the service sarts.
     */
    static void setup(final Mongo pMongo, final DistributedLockSvcOptions pSvcOptions) {
        getDbCollection(pMongo, pSvcOptions).ensureIndex(new BasicDBObject(LockDef.LAST_HEARTBEAT.field, 1), "lastHeartbeatV1Idx", false);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(new BasicDBObject(LockDef.OWNER_APP_NAME.field, 1), "ownerAppNameV1Idx", false);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(new BasicDBObject(LockDef.STATE.field, 1), "stateV1Idx", false);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(new BasicDBObject(LockDef.LOCK_ID.field, 1), "lockIdV1Idx", false);

        final BasicDBObject idStateIdx = new BasicDBObject(LockDef.ID.field, 1);
        idStateIdx.put(LockDef.STATE.field, 1);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(idStateIdx, "idStateV1Idx", false);

        final BasicDBObject stateTimeoutIdx = new BasicDBObject(LockDef.STATE.field, 1);
        stateTimeoutIdx.put(LockDef.LOCK_TIMEOUT_TIME.field, 1);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(stateTimeoutIdx, "stateTimeoutV1Idx", false);

        final BasicDBObject idStateLockIdIdx = new BasicDBObject(LockDef.ID.field, 1);
        idStateLockIdIdx.put(LockDef.LOCK_ID.field, 1);
        idStateLockIdIdx.put(LockDef.STATE.field, 1);
        getDbCollection(pMongo, pSvcOptions).ensureIndex(idStateLockIdIdx, "idStateLockIdV1Idx", false);
    }
}

