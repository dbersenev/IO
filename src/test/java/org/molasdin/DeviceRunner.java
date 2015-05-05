package org.molasdin;

import org.molasdin.io.Device;
import org.molasdin.io.serial.Serial;

import java.io.IOException;

/**
 * Created by molasdin on 5/4/15.
 */
public interface DeviceRunner<T extends Device> {
    void run(T serial) throws IOException;
}
