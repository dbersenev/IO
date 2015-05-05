package org.molasdin;

import org.molasdin.io.serial.Serial;
import org.molasdin.io.serial.manager.SerialManager;

/**
 * Created by molasdin on 5/4/15.
 */
public abstract class BaseTest {

    protected void runWithSerial(DeviceRunner<Serial> runnable){
        try(Serial serial = SerialManager.INSTANCE.create(deviceName())){
            serial.open();
            runnable.run(serial);
        } catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    protected abstract String deviceName();
}
