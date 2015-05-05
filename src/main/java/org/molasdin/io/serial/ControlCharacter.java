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
 * Created by molasdin on 2/18/15.
 */
public enum ControlCharacter {
    IXON(0), IXOFF(1);

    private Integer value;

    private ControlCharacter(Integer value) {
        this.value = value;
    }

    public Integer value(){
        return value;
    }

    public static ControlCharacter fromValue(Integer value){
        for(ControlCharacter entry: ControlCharacter.values()){
            if(entry.value().equals(value)){
                return entry;
            }
        }
        throw new IllegalArgumentException();
    }
}
