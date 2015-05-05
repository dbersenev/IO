/*
 * Copyright 2015 Bersenev Dmitry molasdin@outlook.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.molasdin.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Future;

/**
 * Created by molasdin on 3/21/15.
 */

/**
 * Represents device output
 * with blocking and non blocking capabilities
 */
public interface DeviceOutputChannel extends WritableByteChannel, AsynchronousChannel {
    /**
     * Same as for input
     * Write will be completed if and only if buffer is drained
     * @param exact
     */
    void setExactMode(Boolean exact);
    Boolean isExactMode();

    void setExactAttempts(Long value);

    /**
     * Sets size of data written for single low level write operation
     * Related to Exact mode
     * @param size
     */
    void setExactPartSize(Integer size);
    Integer exactPartSize();

    void setExactDelay(Long micros);
    Long delay();

    Boolean isWriteActive();

    /**
     * Operation is same as defined in NIO packages
     * @param buffer
     * @return
     */
    Future<Integer> writeNoBlock(ByteBuffer buffer);
    <A> Future<Integer> writeNoBlock(final ByteBuffer dst, final A attachment, final CompletionHandler<Integer, ? super A> handler);
}
