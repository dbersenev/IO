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

import org.molasdin.io.serial.*;
import org.molasdin.io.serial.BaudRate;
import org.molasdin.io.serial.manager.SerialManager;
import org.molasdin.io.spi.PinsSPI;
import org.molasdin.io.spi.SPI;

import java.nio.ByteBuffer;

/**
 * Created by molasdin on 3/3/15.
 */
public class App {
    public static void main(String[] argv) throws Exception {
        verifyRW();
    }

    private static void verifySPI() throws Exception{
        try (Serial serial = SerialManager.INSTANCE.create("/dev/cu.Repleo-PL2303-0030131A")) {
            serial.open();

            OutPin mosi = new InvertedOutputPin(serial.outputPinFor(OutputSignal.DTR));
            OutPin sck = new InvertedOutputPin(serial.outputPinFor(OutputSignal.RTS));
            OutPin reset = new InvertedOutputPin(serial.outputPinFor(OutputSignal.TXD));

            InPin miso = new InvertedInputPin(serial.inputPinFor(InputSignal.CTS));

            SPI spi = new PinsSPI(sck, mosi, miso);
            spi.setSCKPeriod(2L);

            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.put((byte) 0xAC);
            buffer.put((byte) 0x53);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.flip();

            ByteBuffer inputBuffer = ByteBuffer.allocate(4);

            reset.pulsate(true, 20, 30);

            spi.output().write(buffer);
            spi.input().read(inputBuffer);

            System.out.printf("%x\n", inputBuffer.get(0));
            System.out.printf("%x\n", inputBuffer.get(1));
            System.out.printf("%x\n", inputBuffer.get(2));
            System.out.printf("%x\n", inputBuffer.get(3));

            buffer.clear();

            buffer.put((byte) 0x30);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.put((byte) 0x00);
            buffer.flip();
            inputBuffer.clear();

            spi.output().write(buffer);
            spi.input().read(inputBuffer);

            System.out.printf("%x\n", inputBuffer.get(0));
            System.out.printf("%x\n", inputBuffer.get(1));
            System.out.printf("%x\n", inputBuffer.get(2));
            System.out.printf("%x\n", inputBuffer.get(3));
        }

    }

    private static void verifySignals() throws Exception{
        try (Serial serial = SerialManager.INSTANCE.create("/dev/cu.Repleo-CH341-0030131A")) {
            serial.open();
            InPin cts = serial.inputPinFor(InputSignal.CTS);
            OutPin dtr = serial.outputPinFor(OutputSignal.DTR);

            cts.addListener((v)->System.out.println(v));

            dtr.setValue(false);

            serial.activatePinListeners();

            Thread.sleep(1);
            dtr.setValue(true);

            Thread.sleep(1);

            dtr.setValue(false);

            Thread.sleep(1);

            dtr.setValue(true);

            Thread.sleep(1);

            dtr.setValue(false);

            Thread.sleep(1000);
        }
    }

    private static void verifyRW() throws Exception{
        try (Serial serial = SerialManager.INSTANCE.create("/dev/cu.Repleo-CH341-0030131A")) {
            serial.open();
            serial.setBaudRate(BaudRate.B9600);
            serial.setFlowControl(FlowControl.HARDWARE);
            ByteBuffer buffer = ByteBuffer.allocateDirect(50);
            DeviceOutputChannel out = serial.output();
            out.write(buffer);
            Thread.sleep(1000);
        }
    }
}
