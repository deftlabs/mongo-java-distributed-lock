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
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * The distributed lock condition object.
 */
public class DistributedLockCondition implements Condition {

    DistributedLockCondition() {

    }

    @Override
    public void await() {

    }

    @Override
    public boolean await(final long pTime, final TimeUnit pUnit) {

        return true;
    }

    @Override
    public long awaitNanos(final long pTime) {

        return 0;
    }

    @Override
    public void awaitUninterruptibly() {

    }

    @Override
    public boolean awaitUntil(final Date pDeadline) {

        return true;
    }

    @Override
    public void signal() {

    }

    @Override
    public void signalAll() {

    }

}

