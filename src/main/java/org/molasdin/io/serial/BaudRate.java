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

package org.molasdin.io.serial;

/**
 * Created by molasdin on 3/22/15.
 */
public enum BaudRate {
    B110(110),
    B300(300),
    B600(600),
    B1200(1200),
    B2400(2400),
    B4800(4800),
    B9600(9600),
    B14400(14400),
    B19200(19200),
    B38400(38400),
    B50K(56000),
    B100K(128000),
    B200K(256000);

    private Integer value;

    private BaudRate(Integer value) {
        this.value = value;
    }

    public Integer value(){
        return value;
    }

    public static BaudRate fromValue(Integer value){
        for(BaudRate entry : values()){
            if(entry.value().equals(value)){
                return entry;
            }
        }
        return null;
    }
}
