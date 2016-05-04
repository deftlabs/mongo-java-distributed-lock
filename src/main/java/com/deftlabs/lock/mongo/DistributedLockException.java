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
 * The distributed lock exception.
 */
public class DistributedLockException extends RuntimeException {

    private static final long serialVersionUID = -4415279469780082174L;

    public DistributedLockException() { super(); }

    public DistributedLockException(final String pMsg) { super(pMsg); }

    public DistributedLockException(final String pMsg, final Throwable pT) { super(pMsg, pT); }

    public DistributedLockException(final Throwable pT) { super(pT); }

}

