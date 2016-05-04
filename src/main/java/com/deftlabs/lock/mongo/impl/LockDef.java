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
 * The distributed lock db data fields.
 */
enum LockDef {
    ID("_id"), // This is the lock name
    LIBRARY_VERSION("libraryVersion"), // A string with the java library version number
    UPDATED("lastUpdated"),
    LAST_HEARTBEAT("lastHeartbeat"),
    LOCK_ACQUIRED_TIME("lockAcquired"), // The time the lock was granted

    STATE("lockState"), // The current state of this lock

    LOCK_ID("lockId"), // The generated id is used to ensure multiple thread/processes don't step on each other

    OWNER_APP_NAME("appName"), // The name of the application who has the lock (optional)

    OWNER_ADDRESS("ownerAddress"),
    OWNER_HOSTNAME("ownerHostname"),
    OWNER_THREAD_ID("ownerThreadId"),
    OWNER_THREAD_NAME("ownerThreadName"),
    OWNER_THREAD_GROUP_NAME("ownerThreadGroupName"),

    INACTIVE_LOCK_TIMEOUT("inactiveLockTimeout"), // The number of ms before timeout (since last heartbeat)
    LOCK_TIMEOUT_TIME("lockTimeoutTime"),

    LOCK_ATTEMPT_COUNT("lockAttemptCount"); // The number of times another thread/process has requested this lock (since locked)

    LockDef(final String pField) { field = pField; }
    final String field;
}

