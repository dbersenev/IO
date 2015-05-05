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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by molasdin on 3/21/15.
 */

/**
 * Only few methods can be implemented to provide output capabilities
 */
public abstract class AbstractDeviceOutputChannel implements DeviceOutputChannel {

    private Long exactAttempts = -1L;
    private Boolean writeActive = false;
    private Boolean exactMode = false;
    private Integer partSize = 0;
    private long exactDelay = 1L;
    private ExecutorService executorService;

    @Override
    public void setExactMode(Boolean exact) {
        this.exactMode = exact;
    }

    @Override
    public Boolean isExactMode() {
        return exactMode;
    }

    @Override
    public void setExactAttempts(Long value) {
        exactAttempts = value;
    }

    @Override
    public void setExactPartSize(Integer size) {
        this.partSize = size;
    }

    @Override
    public Integer exactPartSize() {
        return partSize;
    }

    @Override
    public void setExactDelay(Long millis) {
        this.exactDelay = millis*1000000;
    }

    @Override
    public Long delay() {
        return exactDelay;
    }

    @Override
    public Boolean isWriteActive() {
        return writeActive;
    }

    @Override
    public Future<Integer> writeNoBlock(ByteBuffer buffer) {
        return writeNoBlock(buffer, null, null);
    }

    @Override
    public <A> Future<Integer> writeNoBlock(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
        writeStatus();
        if (executorService == null) {
            executorService = Executors.newFixedThreadPool(1);
        }
        return executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                try {
                    writeActive = true;
                    Integer result = writeData(dst);
                    if (handler != null) {
                        handler.completed(result, attachment);
                    }
                    return result;
                } catch (Exception ex) {
                    if (handler != null) {
                        handler.failed(ex, attachment);
                    }
                } finally {
                    writeActive = false;
                }
                return null;
            }
        });
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return writeData(src);
    }

    @Override
    public void close() throws IOException {
        closeImpl();
    }

    private Integer writeData(ByteBuffer buffer){
        writeStatus();
        writeActive = true;
        if(!exactMode) {
            int result = writeImpl(buffer);
            writeActive = false;
            return result;
        }
        long attempts = exactAttempts;
        boolean firstRun = true;
        int bufferSize = buffer.remaining();
        int total = 0;
        long resultDelay = 0;
        while (total < bufferSize && (attempts == -1L || attempts != 0L)){
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

            if(partSize != 0 && bufferSize > buffer.position() + partSize){
                buffer.limit(buffer.position() + partSize);
            } else if(partSize != 0){
                buffer.limit(bufferSize);
            }

            if(attempts != -1L){
                attempts = attempts - 1;
            }

            total = total + writeImpl(buffer);

            firstRun = false;
        }
        writeActive = false;
        return total;
    }

    private void writeStatus(){
        if(writeActive){
            throw new IllegalStateException("Write is in progress");
        }
    }

    protected abstract void closeImpl();
    protected abstract Integer writeImpl(ByteBuffer buffer);
}
