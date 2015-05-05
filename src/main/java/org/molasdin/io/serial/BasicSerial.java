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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Created by molasdin on 2/20/15.
 */
public class BasicSerial implements Serial {

    private String name;
    private long portHnd;

    private Map<String, InPin> inputPins = new HashMap<String, InPin>();
    private Map<String, OutPin> outputPins = new HashMap<String, OutPin>();

    private DeviceInputChannel input;
    private DeviceOutputChannel output;

    private ByteBuffer changedBuffer;

    private SignalsProcessor signalsProcessor = new SignalsProcessor() {
        protected void analyzeSignals() {
            BasicSerial.this.changedSignals(changedBuffer);
            changed().clear();
            if (changedBuffer.get(0) != 0) {
                int itemsCount = changedBuffer.get(0);
                for (int i = 1; i < itemsCount * 2; i = i + 2) {
                    int id = changedBuffer.get(i);
                    boolean value = changedBuffer.get(i + 1) != 0;
                    changed().put(id, value);
                }
            }
        }

        @Override
        protected void terminateSignalsWait() {
            BasicSerial.this.terminateWaitForSignals();
        }

        @Override
        protected void prepareToCheckSignals(int[] signals) {
            BasicSerial.this.prepareToCheckSignals(signals);
            changedBuffer = ByteBuffer.allocateDirect(signals.length * 2 + 1);
            changedBuffer.order(ByteOrder.LITTLE_ENDIAN);
            changedBuffer.put(0, (byte)0);
        }
    };

    static {
        LibraryLoader loader = new LibraryLoader("serial");
        loader.load();
    }


    public BasicSerial(String name) {
        this.name = name;
    }

    @Override
    public void open() {
        if(name() == null || name().isEmpty()){
            throw new RuntimeException("Port name is empty");
        }
        portHnd = openPort(name());
        recreatePins();
        checkOpen();
    }

    @Override
    public Boolean isOpen() {
        return portHnd() != 0;
    }

    @Override
    public InPin inputPinFor(InputSignal signal) {
        checkOpen();
        return inputPins.get(signal.name());
    }

    @Override
    public void activatePinListeners() {
        signalsProcessor.start();
    }

    @Override
    public void stopPinListeners() {
        signalsProcessor.stop();
    }

    @Override
    public OutPin outputPinFor(OutputSignal signal) {
        checkOpen();
        return outputPins.get(signal.name());
    }

    @Override
    public void setReadTimeout(Long millis) {
        setPortReadTimeout(millis);
    }

    @Override
    public void setBaudRate(BaudRate baudRate) {
        checkOpen();
        setPortBaud(baudRate.value());
    }

    @Override
    public BaudRate baudRate() {
        checkOpen();
        return BaudRate.fromValue(portBaud());
    }

    @Override
    public void setDataBits(DataBits dataBits) {
        checkOpen();
        setPortBits(dataBits.value());
    }

    @Override
    public DataBits dataBits() {
        checkOpen();
        return DataBits.fromValue(portBits());
    }

    @Override
    public void setStopBits(StopBits stopBits) {
        checkOpen();
        setPortStopBits(stopBits.value());
    }

    @Override
    public StopBits stopBits() {
        checkOpen();
        return StopBits.fromValue(portStopBits());
    }

    @Override
    public void setParity(Parity parity) {
        checkOpen();
        setPortParity(parity.value());
    }

    @Override
    public Parity parity() {
        checkOpen();
        return Parity.fromValue(portParity());
    }

    @Override
    public void setFlowControl(FlowControl flowControl) {
        checkOpen();
        setPortFlowCtl(flowControl.value());
    }

    @Override
    public FlowControl flowControl() {
        checkOpen();
        return FlowControl.fromValue(portFlowCtl());
    }

    @Override
    public void setControlCharacter(ControlCharacter controlCharacter, Character value) {
        checkOpen();
        setPortChar(controlCharacter.value(), (byte) value.charValue());
    }

    @Override
    public Character controlCharacter(ControlCharacter controlCharacter) {
        checkOpen();
        return (char) portChar(controlCharacter.value());
    }

    @Override
    public void close() throws IOException {
        checkOpen();
        signalsProcessor.stop();
        if (input != null) {
            input.close();
        }
        if (output != null) {
            output.close();
        }
        closePort();
        portHnd = 0;
    }

    @Override
    public DeviceInputChannel input() {
        checkOpen();
        if (input == null) {
            input = new AbstractDeviceInputChannel() {
                @Override
                protected Integer readImpl(ByteBuffer buffer) {
                    int total = BasicSerial.this.read(buffer, buffer.remaining());
                    if(total != 0){
                        buffer.position(total);
                    }
                    return total;
                }

                @Override
                public boolean isOpen() {
                    return input != null;
                }

                @Override
                public void closeImpl() {
                    input = null;
                }
            };
        }
        return input;
    }

    @Override
    public DeviceOutputChannel output() {
        checkOpen();
        if (output == null) {
            output = new AbstractDeviceOutputChannel() {
                @Override
                protected void closeImpl() {
                    output = null;
                }

                @Override
                protected Integer writeImpl(ByteBuffer buffer) {
                    int total =  BasicSerial.this.write(buffer, buffer.remaining());
                    if(total != 0){
                        buffer.position(total);
                    }
                    return total;
                }

                @Override
                public boolean isOpen() {
                    return output != null;
                }
            };
        }
        return output;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Boolean hasPin(String name) {
        checkOpen();
        return inputPins.containsKey(name) || outputPins.containsKey(name);
    }

    @Override
    public Map<String, InPin> inputPins() {
        checkOpen();
        return inputPins;
    }

    @Override
    public Map<String, OutPin> outputPins() {
        checkOpen();
        return outputPins;
    }


    private void checkOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("Port is closed");
        }
    }

    private void recreatePins() {
        signalsProcessor.stop();
        for (final OutputSignal signal : OutputSignal.values()) {
            OutPin pin = new OutPin() {
                @Override
                public void setValue(Boolean value) {
                    setPinSignal(signal.value(), value);
                }

                @Override
                public String name() {
                    return signal.name();
                }
            };
            outputPins.put(signal.name(), pin);
        }

        for (final InputSignal signal : InputSignal.values()) {
            InPin pin = new InPin() {
                @Override
                public Boolean value() {
                    return pinSignal(signal.value());
                }

                @Override
                public void addListener(InputPinListener listener) {
                    addPinListener(this, listener);
                }

                @Override
                public void removeListener(InputPinListener listener) {
                    removePinListener(this, listener);
                }

                @Override
                public String name() {
                    return signal.name();
                }
            };

            inputPins.put(signal.name(), pin);
        }
    }

    private void addPinListener(InPin pin, InputPinListener listener) {
        signalsProcessor.addPinListener(listener, InputSignal.valueOf(pin.name()).value());
    }

    private void removePinListener(InPin pin, InputPinListener listener) {
        signalsProcessor.removePinListener(InputSignal.valueOf(pin.name()).value(), listener);
    }

    protected long portHnd() {
        return portHnd;
    }

    private native long openPort(String name);

    private native void closePort();

    private native void setPortBaud(int baud);

    private native int portBaud();

    private native void setPortBits(int bits);

    private native int portBits();

    private native void setPortStopBits(int bits);

    private native int portStopBits();

    private native void setPortParity(int parity);

    private native int portParity();

    private native void setPortFlowCtl(int ctl);

    private native int portFlowCtl();

    private native void setPortChar(int id, byte value);

    private native byte portChar(int id);

    private native void setPortReadTimeout(long timeout);

    private native int read(ByteBuffer buffer, int size);

    private native int write(ByteBuffer buffer, int size);

    private native void setPinSignal(int sig, boolean value);

    private native boolean pinSignal(int sig);

    private native void prepareToCheckSignals(int[] signals);

    private native void changedSignals(ByteBuffer changed);

    private native void terminateWaitForSignals();

}
