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

/**
 * Created by molasdin on 2/17/15.
 */
public interface OutPin extends Pin {
    void setValue(Boolean value);

    /**
     * Generates pulse
     * @param start   initial pulse value
     * @param delay1  delay after initial value was set
     * @param delay2  delay after initial value was inverted
     */
    default void pulsate(Boolean start, long delay1, long delay2){
        setValue(start);
        Sleep.sleepMillis(delay1);
        setValue(!start);
        Sleep.sleepMillis(delay2);
    }
}
