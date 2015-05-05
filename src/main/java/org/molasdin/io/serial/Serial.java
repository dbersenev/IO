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

import org.molasdin.io.*;

import java.io.Closeable;

/**
 * Created by molasdin on 2/17/15.
 */
public interface Serial extends InputDevice, OutputDevice,
        DeviceWithInputPins, DeviceWithOutputPins,
        Openable, Closeable{
    InPin inputPinFor(InputSignal signal);
    OutPin outputPinFor(OutputSignal signal);

    void setReadTimeout(Long millis);

    void setBaudRate(BaudRate baudRate);
    BaudRate baudRate();

    void setDataBits(DataBits dataBits);
    DataBits dataBits();

    void setStopBits(StopBits stopBits);
    StopBits stopBits();

    void setParity(Parity parity);
    Parity parity();

    void setFlowControl(FlowControl flowControl);
    FlowControl flowControl();

    void setControlCharacter(ControlCharacter controlCharacter, Character value);
    Character controlCharacter(ControlCharacter controlCharacter);
}
