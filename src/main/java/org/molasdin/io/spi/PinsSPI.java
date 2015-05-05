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

package org.molasdin.io.spi;

import org.molasdin.io.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by molasdin on 3/25/15.
 */

/**
 * Very simple implementation
 * Supports only Master mode without "Slave Select"
 */
public class PinsSPI implements SPI {

    private OutPin sck;
    private OutPin mosi;
    private InPin miso;
    private Boolean master = true;
    private Boolean cpol = false;
    private Boolean cpha = false;
    private long sckPeriod = 1L;
    private ByteBuffer buffer;

    private Map<String, Pin> pins = new HashMap<>(3);

    private DeviceInputChannel input;
    private DeviceOutputChannel output;

    public PinsSPI(OutPin sck, OutPin mosi, InPin miso) {
        this.sck = sck;
        this.mosi = mosi;
        this.miso = miso;
        pins.put(sck.name(), sck);
        pins.put(mosi.name(), mosi);
        pins.put(miso.name(), miso);
        init();
    }

    private void init(){
        sck.setValue(cpol);
        mosi.setValue(false);
    }

    @Override
    public void setSCKPeriod(Long period) {
        this.sckPeriod = period;
    }

    @Override
    public void setMaster(Boolean flag) {
        this.master = flag;
        init();
    }

    @Override
    public void setCPOL(Boolean flag) {
        this.cpol = flag;
        init();
    }

    @Override
    public void setCPHA(Boolean flag) {
        this.cpha = flag;
    }

    @Override
    public void close() throws IOException {
        if(input != null){
            input.close();
        }

        if(output != null){
            output.close();
        }
    }


    @Override
    public DeviceInputChannel input() {
        if(input == null){
            input = new AbstractDeviceInputChannel() {
                @Override
                protected Integer readImpl(ByteBuffer data) {
                    if(master){
                        if(buffer != null){
                            int total = data.remaining();
                            data.put(buffer);
                            buffer = null;
                            return total;
                        }
                    }
                    return 0;
                }

                @Override
                protected void closeImpl() {

                }

                @Override
                public boolean isOpen() {
                    return true;
                }
            };
        }
        return input;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public Boolean hasPin(String name) {
        return pins.containsKey(name);
    }

    @Override
    public void open() {

    }

    @Override
    public Boolean isOpen() {
        return null;
    }

    @Override
    public DeviceOutputChannel output() {
        if(output == null){
            output = new AbstractDeviceOutputChannel() {
                @Override
                protected void closeImpl() {

                }

                @Override
                protected Integer writeImpl(ByteBuffer data) {
                    if(master){
                        int total = data.remaining();
                        PinsSPI.this.write(data);
                        return total;
                    }
                    return 0;
                }

                @Override
                public boolean isOpen() {
                    return true;
                }
            };
        }
        return output;
    }

    private void write(ByteBuffer data){
        buffer = ByteBuffer.allocate(data.remaining());
        while (data.remaining() > 0){
            byte item = data.get();
            int cnt = 0;
            while(cnt < 8){
                mosi.setValue((item & 0x80) != 0);
                item = (byte)((item & 0xFF) << 1);
                sck.setValue(true);
                Sleep.sleepMillis(sckPeriod);

                if(miso.value()){
                    item = (byte)(item | 0x01);
                } else {
                    item = (byte)(item & 0xFE);
                }
                sck.setValue(false);
                Sleep.sleepMillis(sckPeriod);
                cnt ++;
            }
            buffer.put(item);
        }
        buffer.flip();
    }
}
