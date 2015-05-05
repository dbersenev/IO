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

#ifdef TEST_RUN

#include "serial.h"

#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <thread>
#include <chrono>



int main(int argc, const char * argv[]) {
    unsigned char buffer[3] = {1,2,3};
    unsigned char result[3] = {0, 0, 0};
    Serial *serial = new BasicSerial("COM3");
    serial->open();
    serial->setBaudRate(9600);
    serial->setDataBits(Serial::DataBits::DBCS8);
    serial->setParity(Serial::Parity::EVEN);
    serial->setFlowControl(Serial::FlowControl::HARDWARE);
    serial->setReadTimeout(5000);
    serial->write(buffer, 3);
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    serial->read(result, 3);
    printf("%d, %d, %d\n", result[0], result[1], result[2]);
    delete serial;
    return 0;
}

#endif
