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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;

/**
 * Created by molasdin on 3/10/15.
 */

/**
 * Only few methods can be implemented to provide input capabilities
 */
public abstract class AbstractDeviceInputChannel implements DeviceInputChannel {

    private Boolean exactMode = false;
    private Long exactAttempts = -1L;
    private long exactDelay = 1L;
    private Boolean readActive = false;
    private ExecutorService executorService;

    @Override
    public void setExactMode(Boolean flag) {
        this.exactMode = flag;
    }

    @Override
    public void setExactAttempts(Long value) {
        exactAttempts = value;
    }

    @Override
    public void setExactDelay(Long micros) {
        this.exactDelay = micros * 1000000;
    }

    @Override
    public Future<Integer> readNoBlock(ByteBuffer buffer) {
        return readNoBlock(buffer, null, null);
    }

    @Override
    public Boolean isReadActive() {
        return readActive;
    }

    @Override
    public <A> Future<Integer> readNoBlock(final ByteBuffer dst, final A attachment, final CompletionHandler<Integer, ? super A> handler) {
        readStatus();
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(1);
        }
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    Integer result = readData(dst);
                    if (handler != null) {
                        handler.completed(result, attachment);
                    }
                    return result;
                } catch (Exception ex) {
                    if (handler != null) {
                        handler.failed(ex, attachment);
                    }
                } finally {
                    readActive = false;
                }
                return null;
            }
        });
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return readData(dst);
    }

    @Override
    public void close() throws IOException {
        try {
            if (executorService != null) {
                executorService.shutdown();
                try {
                    executorService.awaitTermination(5L, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(!executorService.isTerminated()){
                    executorService.shutdownNow();
                }
            }
        } finally {
            executorService = null;
            closeImpl();
        }
    }

    private Integer readData(ByteBuffer buffer) {
        readStatus();
        readActive = true;
        try {
            if (!exactMode) {
                return readImpl(buffer);
            }
            int total = 0;
            long attempts = exactAttempts;
            boolean firstRun = true;
            long resultDelay = 0;
            while (buffer.remaining() != 0 && (attempts == -1L || attempts != 0L)) {
                if(!firstRun){
                    if(exactDelay == 0){
                        Thread.yield();
                    } else {
                        resultDelay = System.nanoTime() + exactDelay;
                        while(resultDelay > System.nanoTime()){
                            Thread.yield();
                        }
                    }
                }
                if (attempts != -1L) {
                    attempts = attempts - 1;
                }
                total = total + readImpl(buffer);
                firstRun = false;
            }
            return total;
        } finally {
            readActive = false;
        }

    }

    private void readStatus() {
        if (readActive) {
            throw new IllegalStateException("Read is in progress");
        }
    }

    protected abstract Integer readImpl(ByteBuffer buffer);

    protected abstract void closeImpl();
}
