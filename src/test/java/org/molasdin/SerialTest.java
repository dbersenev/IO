package org.molasdin;

import org.junit.Assert;
import org.junit.Test;
import org.molasdin.io.*;
import org.molasdin.io.serial.*;
import org.molasdin.io.serial.BaudRate;
import org.molasdin.io.serial.manager.SerialManager;
import org.molasdin.io.util.ByteUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by molasdin on 3/22/15.
 */
public class SerialTest extends BaseTest{

    @Override
    protected String deviceName() {
        return System.getProperty("serial");
    }

    @Test
    public void testConfig() throws Exception{
        runWithSerial(serial -> {
            for(BaudRate entry: BaudRate.values()){
                serial.setBaudRate(entry);
                Assert.assertEquals(entry, serial.baudRate());
            }

            for(DataBits entry: DataBits.values()){
                serial.setDataBits(entry);
                Assert.assertEquals(entry, serial.dataBits());
            }

            for(StopBits entry: StopBits.values()){
                serial.setStopBits(entry);
                Assert.assertEquals(entry, serial.stopBits());
            }

            for(FlowControl entry: FlowControl.values()){
                serial.setFlowControl(entry);
                Assert.assertEquals(entry, serial.flowControl());
            }

            for(Parity entry : Parity.values()){
                serial.setParity(entry);
                Assert.assertEquals(entry, serial.parity());
            }
        });
    }

    @Test
    public void testWriteRead(){
        runWithSerial(serial ->{
            serial.setBaudRate(BaudRate.B9600);
            serial.setFlowControl(FlowControl.HARDWARE);
            ByteBuffer outBuffer = ByteUtils.INSTANCE.newDirectBufferWithData(1,2,3,4,5);
            ByteBuffer inBuffer = ByteBuffer.allocateDirect(5);
            DeviceOutputChannel out = serial.output();
            DeviceInputChannel in = serial.input();
            in.setExactMode(true);
            out.write(outBuffer);
            Sleep.sleepMillis(10);
            in.read(inBuffer);
            inBuffer.flip();
            outBuffer.flip();
            Assert.assertEquals(outBuffer, inBuffer);
        });
    }

    @Test
    public void testPins(){
        runWithSerial(serial -> {
            List<Boolean> outputs = Arrays.asList(true, false, true, false, true, false);
            List<Boolean> results = new ArrayList<>(outputs.size());
            InPin cts = serial.inputPinFor(InputSignal.CTS);
            OutPin dtr = serial.outputPinFor(OutputSignal.DTR);
            dtr.setValue(false);
            cts.addListener(results::add);
            serial.activatePinListeners();
            for (Boolean entry: outputs){
                dtr.setValue(entry);
                Sleep.sleepMillis(10);
            }
            Assert.assertEquals(outputs, results);
        });
    }

}
