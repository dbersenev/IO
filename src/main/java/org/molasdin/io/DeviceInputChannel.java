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
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Future;

/**
 * Created by molasdin on 3/10/15.
 */

/**
 * Represents device input
 * Supports blocking and non blocking io
 */
public interface DeviceInputChannel extends ReadableByteChannel, AsynchronousChannel {
    /**
     * Enables reading of exact amount data from device.
     * Read blocks untill all of the data is retrieved or attempts are exhausted.
     * @param flag
     */
    void setExactMode(Boolean flag);

    /**
     * Set attempts for exact reading mode
     * @param value
     */
    void setExactAttempts(Long value);

    /**
     * Delay between each low level read operation when exact
     * amount of data is being retrieved
     * @param micros
     */
    void setExactDelay(Long micros);

    /**
     * Allows to check if any read is active in this channel
     * @return
     */
    Boolean isReadActive();

    /**
     * Operation is same as defined in NIO packages
     * @param buffer
     * @return
     */
    Future<Integer> readNoBlock(ByteBuffer buffer);
    <A> Future<Integer> readNoBlock(ByteBuffer dst, A attachment, CompletionHandler<Integer,? super A> handler);
}
