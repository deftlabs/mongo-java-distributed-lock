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
import org.bson.types.ObjectId;

// Java
import java.util.concurrent.locks.Lock;

/**
 * The distributed lock interface.
 */
public interface DistributedLock extends Lock {

    /**
     * Returns true if the lock is currently locked by the local process.
     */
    public boolean isLocked();

    /**
     * Returns the lock name.
     */
    public String getName();

    /**
     * Returns the lock id if current locked (null if not).
     */
    public ObjectId getLockId();

    /**
     * Returns the options used to configure this lock.
     */
    public DistributedLockOptions getOptions();

    /**
     * Wakeup any blocked threads. This should <b>ONLY</b> be used by the lock service,
     * not the user.
     */
    public void wakeupBlocked();

}

