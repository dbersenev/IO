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

package org.molasdin.io.serial.manager;

import org.molasdin.io.serial.BasicSerial;
import org.molasdin.io.serial.Serial;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by molasdin on 3/21/15.
 */

/**
 * Provides access to serial devices.
 * Devices are cached.
 * If some of devices are left open they will be closed on JVM exit
 */
public enum SerialManager {
    INSTANCE;

    private Map<String, Serial> devices = new HashMap<>();
    private Boolean hasHook = false;

    public Serial create(String name){
        if(devices.containsKey(name)){
            return devices.get(name);
        }

        Serial serial = new BasicSerial(name);
        try {
            serial.open();
            serial.close();
        } catch (Exception ex){
            throw new RuntimeException("Serial device is invalid", ex);
        }

        devices.put(name, serial);
        if(!hasHook){
            hasHook = true;
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    close();
                }
            });
        }
        return serial;
    }

    public void close(){
        List<Exception> exceptions = new LinkedList<>();
        for(String name: devices.keySet()){
            Serial serial = devices.get(name);
            if(serial.isOpen()){
                try {
                    serial.close();
                } catch (Exception ex) {
                    exceptions.add(ex);
                }
            }
        }
        devices.clear();
        if(!exceptions.isEmpty()){
            RuntimeException exception = new RuntimeException();
            exceptions.forEach(exception::addSuppressed);
            throw exception;
        }
    }



}
