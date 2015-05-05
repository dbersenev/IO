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
 * Created by molasdin on 2/17/15.
 */
public enum Parity {
    DISABLED(0), EVEN(2), ODD(1);

    private Integer value;

    private Parity(Integer value) {
        this.value = value;
    }

    public Integer value(){
        return value;
    }

    public static Parity fromValue(Integer value){
        if(value == 0){
            return DISABLED;
        } else if (value == 1){
            return ODD;
        } else {
            return EVEN;
        }
    }
}
