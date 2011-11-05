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
 * The distributed lock history fields. See LockDef for more info.
 *
 * The fields in LockDef need to be represented here.
 */
enum LockHistoryDef {

    ID("_id"), // This is an object id in history
    LIBRARY_VERSION("libraryVersion"),
    STATE("lockState"),
    LOCK_ID("lockId"),
    OWNER_APP_NAME("appName"),
    OWNER_ADDRESS("ownerAddress"),
    OWNER_HOSTNAME("ownerHostname"),
    OWNER_THREAD_ID("ownerThreadId"),
    OWNER_THREAD_NAME("ownerThreadName"),
    OWNER_THREAD_GROUP_NAME("ownerThreadGroupName"),
    INACTIVE_LOCK_TIMEOUT("inactiveLockTimeout"),


    // Fields on top of LockDef
    CREATED("historyCreated"),
    LOCK_NAME("lockName"),
    TIMED_OUT("timedOut");

    private LockHistoryDef(final String pField) { field = pField; }
    final String field;
}

