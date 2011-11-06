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

/**
 * The lock monitors.
 */
final class Monitor {

    /**
     * The lock heartbeat thread is responsible for sending updates to the lock
     * doc every X seconds (when the lock is owned by the current process). This
     * library uses missing/stale/old heartbeats to timeout locks that have not been
     * closed properly (based on the lock/unlock) contract. This can happen when processes
     * die unexpectedly (e.g., out of memory) or when they are not stopped properly (e.g., kill -9).
     */
    static class LockHeartbeat implements Runnable {
        @Override
        public void run() {

        }
    }

    /**
     * The lock timeout thread impl (see LockHeartbeat docs for more info). One lock
     * timeout thread runs in each process this lock lib is running. This thread is
     * responsible for cleaning up expired locks (based on time since last heartbeat).
     */
    static class LockTimeout implements Runnable {
        @Override
        public void run() {

        }
    }
}

