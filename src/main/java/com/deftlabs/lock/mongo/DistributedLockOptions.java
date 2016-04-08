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
 * The options/configuration on a per lock basis. If this is not passed when creating the lock,
 * the library defaults are used.
 */
public class DistributedLockOptions {

    /**
     * Set the inactive lock timeout (time in milliseconds). The default is one minute.
     * This means that if your lock process dies or is killed without unlocking first,
     * the lock will be reset in one minute (120,000 ms).
     */
    public void setInactiveLockTimeout(final int pV) { _inactiveLockTimeout = pV; }

    /**
     * Returns the inactive lock timeout.
     */
    public int getInactiveLockTimeout() { return _inactiveLockTimeout; }

    private int _inactiveLockTimeout = 120000;
}

