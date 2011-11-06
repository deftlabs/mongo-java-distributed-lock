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

/**
 * The distributed lock service interface.
 */
public interface DistributedLockSvc {

    /**
     * Create a new distributed lock. If a lock already exists with this name,
     * the lock will be returned (else a new one is created).
     */
    public DistributedLock create(final String pLockName);

    /**
     * Create a new distributed lock. If a lock already exists with this name,
     * the lock will be returned (else a new one is created).
     */
    public DistributedLock create(final String pLockName, final DistributedLockOptions pOptions);

    /**
     * Destroy a distributed lock.
     */
    public void destroy(final DistributedLock pLock);

    /**
     * Part of the usage contract. The startup method is called by the factory.
     */
    public void startup();

    /**
     * Part of the usage contract. Must be called when the app stops.
     */
    public void shutdown();

    /**
     * Returns true if the lock service is running.
     */
    public boolean isRunning();

}

