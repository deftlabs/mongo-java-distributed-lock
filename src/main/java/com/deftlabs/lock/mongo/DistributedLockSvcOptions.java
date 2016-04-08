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

// Java
import com.mongodb.MongoClient;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * The global distributed lock service options/config.
 */
public class DistributedLockSvcOptions {

    /**
     * The basic constructor. This uses the following:<br />
     * <ul>
     * <li>database name: mongo-distributed-lock
     * <li>collection name: locks
     * </ul>
     *
     */
    public DistributedLockSvcOptions(final String pMongoUri)
    { this(pMongoUri, "mongo-distributed-lock", "locks", null);
    }

    /**
     * Constructor that allows the user to specify database and colleciton name.
     */
    public DistributedLockSvcOptions(   final String pMongoUri,
                                        final String pDbName,
                                        final String pCollectionName)
    { this(pMongoUri, pDbName, pCollectionName, null);
    }

    /**
     * Constructor that allows the user to specify database, colleciton and app name.
     * The app name should definetly be used if the db/collection names are shared by multiple
     * apps/systems (e.g., SomeCoolDataProcessor).
     */
    public DistributedLockSvcOptions(   final String pMongoUri,
                                        final String pDbName,
                                        final String pCollectionName,
                                        final String pAppName)
    {
        _mongoUri = pMongoUri;
        _dbName = pDbName;
        _collectionName = pCollectionName;
        _appName = pAppName;

        try { _hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) { _hostAddress = null; }

        try { _hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) { _hostname = null; }
        _mongoClient = null;
    }

    /**
     * The basic constructor. This uses the following:<br />
     * <ul>
     * <li>database name: mongo-distributed-lock
     * <li>collection name: locks
     * </ul>
     *
     */
    public DistributedLockSvcOptions(final MongoClient pMongoClient)
    { this(pMongoClient, "mongo-distributed-lock", "locks", null);
    }

    /**
     * Constructor that allows the user to specify database and colleciton name.
     */
    public DistributedLockSvcOptions(   final MongoClient pMongoClient,
                                        final String pDbName,
                                        final String pCollectionName)
    { this(pMongoClient, pDbName, pCollectionName, null);
        }

    /**
     * Constructor that allows the user to specify database, colleciton and app name.
     * The app name should definetly be used if the db/collection names are shared by multiple
     * apps/systems (e.g., SomeCoolDataProcessor).
     */
    public DistributedLockSvcOptions(   final MongoClient pMongoClient,
                                        final String pDbName,
                                        final String pCollectionName,
                                        final String pAppName)
    {
        _mongoClient = pMongoClient;
        _dbName = pDbName;
        _collectionName = pCollectionName;
        _appName = pAppName;

        try { _hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) { _hostAddress = null; }

        try { _hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) { _hostname = null; }
        _mongoUri = null;
    }

    public MongoClient getMongoClient() { return _mongoClient; }
    public String getMongoUri() { return _mongoUri; }
    public String getDbName() { return _dbName; }
    public String getCollectionName() { return _collectionName; }
    public String getAppName() { return _appName; }

    /**
     * True by default. Set to false to disable storing historical data. Lock data
     * is tracked when individual locks are locked, unlocked and (possibly) timed out.
     */
    public void setEnableHistory(final boolean pV) { _enableHistory = pV; }
    public boolean getEnableHistory() { return _enableHistory; }

    /**
     * True by default. Set to fault to use a regular collection instead of a
     * capped collection for lock history. Entries will never be deleted if
     * this is not a capped collection.
     */
    public void setHistoryIsCapped(final boolean pV) { _historyIsCapped = pV; }
    public boolean getHistoryIsCapped() { return _historyIsCapped; }

    /**
     * Set the size (in bytes) of the historical capped collection. The default
     * is 200MB.
     */
    public void setHistorySize(final long pV) { _historySize = pV; }
    public long getHistorySize() { return _historySize; }

    public String getLibVersion() { return LIB_VERSION; }

    public String getHostname() { return _hostname; }
    public void setHostname(final String pV) { _hostname = pV; }

    public String getHostAddress() { return _hostAddress; }

    /**
     * Milliseconds between heartbeat checks.
     */
    public long getHeartbeatFrequency() { return _heartbeatFrequency; }
    public void setHeartbeatFrequency(final long pHeartbeatFrequency) {
        _heartbeatFrequency = pHeartbeatFrequency;
    }

    /**
     * Milliseconds between lock timeout checks.
     */
    public long getTimeoutFrequency() { return _timeoutFrequency; }
    public void setTimeoutFrequency(final long pTimeoutFrequency) {
        _timeoutFrequency = pTimeoutFrequency;
    }

    /**
     * Milliseconds between lock unlocked checks.
     */
    public long getLockUnlockedFrequency() { return _lockUnlockedFrequency; }
    public void setLockUnlockedFrequency(final long pLockUnlockedFrequency) {
        _lockUnlockedFrequency = pLockUnlockedFrequency;
    }

    /**
     * The default collection name is: lockHistory. Override here.
     */
    public void setHistoryCollectionName(final String pV) { _historyCollectionName = pV; }
    public String getHistoryCollectionName() { return _historyCollectionName; }

    private final MongoClient _mongoClient;
    private final String _mongoUri;
    private final String _dbName;
    private final String _collectionName;
    private String _historyCollectionName = "lockHistory";

    private String _hostname;
    private String _hostAddress;

    private final String _appName;

    private static final String LIB_VERSION = "@LIB_VERSION@";

    private boolean _enableHistory = true;
    private boolean _historyIsCapped = true;
    private long _historySize = 209715200;

    private long _heartbeatFrequency = 5000;
    private long _timeoutFrequency = 60000;
    private long _lockUnlockedFrequency = 1000;
}

