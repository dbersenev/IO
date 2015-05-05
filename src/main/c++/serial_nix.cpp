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

#ifdef SERIAL_NIX

#include <stdio.h>
#include <termios.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <sys/select.h>
#include <sys/types.h>

#include "serial.h"


class NixSerial:public Serial{
private:
    int fd;
    termios options;
public:
    NixSerial(const char *name):Serial(name){
       
    }
    
    virtual ~NixSerial() {
        if(isOpen()){
            terminateSignalsWait();
            close();
        }
    }
    
    virtual void open() override{
        fd = ::open(name(), O_RDWR | O_NOCTTY | O_NDELAY);
        if(fd == -1){
            throw CanNotOpenPortException();
        }
        fcntl(fd, F_SETFL, 0);
        termios options;
        tcgetattr(fd, &options);
        cfmakeraw(&options);
        tcsetattr(fd, TCSANOW, &options);
        setBaudRate(110);
    }
    
    virtual void close() override{
        ::close(fd);
        fd = 0;
    }
    
    virtual bool isOpen() override{
        return fd != 0;
    }
    
    virtual void setBaudRate(int baudRate) override{
        readOptions();
        speed_t tmp = B110;
        switch (baudRate) {
            case 110:
                tmp = B110;
                break;
            case 300:
                tmp = B300;
                break;
            case 600:
                tmp = B600;
                break;
            case 1200:
                tmp = B1200;
                break;
            case 2400:
                tmp = B2400;
                break;
            case 4800:
                tmp = B4800;
                break;
            case 9600:
                tmp = B9600;
                break;
            case 14400:
                tmp = B14400;
                break;
            case 19200:
                tmp = B19200;
                break;
            case 38400:
                tmp = B38400;
                break;
            case 56000:
                tmp = B57600;
                break;
            case 128000:
                tmp = B115200;
                break;
            case 256000:
                tmp = B230400;
                break;
        }
        cfsetispeed(&options, tmp);
        cfsetospeed(&options, tmp);
        applyChanges();
    }
    virtual int baudRate() override{
        readOptions();
        speed_t baudRate = cfgetispeed(&options);
        int tmp = 0;
        switch (baudRate) {
            case B110:
                tmp = 110;
                break;
            case B300:
                tmp = 300;
                break;
            case B600:
                tmp = 600;
                break;
            case B1200:
                tmp = 1200;
                break;
            case B2400:
                tmp = 2400;
                break;
            case B4800:
                tmp = 4800;
                break;
            case B9600:
                tmp = 9600;
                break;
            case B14400:
                tmp = 14400;
                break;
            case B19200:
                tmp = 19200;
                break;
            case B38400:
                tmp = 38400;
                break;
            case B57600:
                tmp = 56000;
                break;
            case B115200:
                tmp = 128000;
                break;
            case B230400:
                tmp = 256000;
                break;
        }

        return tmp;
    }
    
    virtual void setDataBits(Serial::DataBits dataBits) override{
        readOptions();
        options.c_cflag &= ~CSIZE;
        tcflag_t flag = 0;
        switch (dataBits) {
            case DataBits::DBCS5:
                flag = CS5;
                break;
            case DataBits::DBCS6:
                flag = CS6;
                break;
            case DataBits::DBCS7:
                flag = CS7;
                break;
            case DataBits::DBCS8:
                flag = CS8;
        }
        options.c_cflag |= flag;
        applyChanges();
    }
    
    virtual DataBits dataBits() override{
        readOptions();
        options.c_cflag &= CSIZE;
        if(options.c_cflag == CS8){
            return DataBits::DBCS8;
        } else if(options.c_cflag == CS7){
            return DataBits::DBCS7;
        } else if(options.c_cflag == CS6){
            return DataBits::DBCS6;
        } else {
            return DataBits::DBCS5;
        }
    }
    
    virtual void setStopBits(StopBits bits) override{
        readOptions();
        if (bits == StopBits::ONE){
            options.c_cflag &= ~ CSTOPB;
        } else{
            options.c_cflag |= CSTOPB;
        }
        applyChanges();
    }
    virtual StopBits stopBits() override{
        readOptions();
        if (options.c_cflag&CSTOPB){
            return StopBits::TWO;
        }
        return StopBits::ONE;
    }
    
    virtual void setParity(Parity parity) override{
        readOptions();
        switch (parity) {
            case Parity::EVEN:
                options.c_iflag |= (INPCK | ISTRIP);
                options.c_cflag |= PARENB;
                options.c_cflag &= ~PARODD;
                break;
            case Parity::ODD:
                options.c_iflag |= (INPCK | ISTRIP);
                options.c_cflag |= PARENB;
                options.c_cflag |= PARODD;
                break;
            default:
                options.c_cflag &= ~PARENB;
                options.c_iflag &= ~INPCK;
                break;
        }
        applyChanges();
    }
    virtual Parity parity() override{
        readOptions();
        if (options.c_cflag & PARENB){
            if (options.c_cflag & PARODD){
                return Parity::ODD;
            } else {
                return Parity::EVEN;
            }
        } else {
            return Parity::DISABLED;
        }
    }
    
    virtual void setFlowControl(FlowControl flowControl) override{
        readOptions();
        switch (flowControl) {
            case FlowControl::NONE:
                options.c_cflag &= ~CRTSCTS;
                options.c_iflag &= ~(IXON | IXOFF);
                break;
            case FlowControl::HARDWARE:
                options.c_cflag |= CRTSCTS;
                options.c_iflag &= ~(IXON | IXOFF);
                break;
            case FlowControl::SOFTWARE_XON:
                options.c_cflag &= ~CRTSCTS;
                options.c_iflag &= ~(IXON | IXOFF);
                options.c_iflag |= IXON;
                break;
            case FlowControl::SOFTWARE_XOFF:
                options.c_cflag &= ~CRTSCTS;
                options.c_iflag &= ~(IXON | IXOFF);
                options.c_iflag |= IXOFF;
                break;
            case FlowControl::SOFTWARE_BOTH:
                options.c_cflag &= ~CRTSCTS;
                options.c_iflag |= (IXON|IXOFF);
            default:
                break;
        }
        applyChanges();
        
    }
    virtual FlowControl flowControl() override{
        readOptions();
        if(!(options.c_iflag & IXON) && !(options.c_iflag & IXOFF)
           && !(options.c_cflag & CRTSCTS)){
            return FlowControl::NONE;
        } else if(options.c_cflag & CRTSCTS){
            return FlowControl::HARDWARE;
        } else if((options.c_iflag & IXON) && !(options.c_iflag&IXOFF)){
            return FlowControl::SOFTWARE_XON;
        } else if((options.c_iflag&IXOFF) && !(options.c_iflag&IXON)){
            return FlowControl::SOFTWARE_XOFF;
        } else {
            return FlowControl::SOFTWARE_BOTH;
        }
    }
    
    virtual void setControlCharacter(ControlCharacter name, uint8_t value) override{
        readOptions();
        if(name == ControlCharacter::FIXON){
            options.c_cc[VSTART] = value;
        } else if (name == ControlCharacter::FIXOFF){
            options.c_cc[VSTOP] = value;
        }
        applyChanges();
    }
    virtual uint8_t controlCharacter(ControlCharacter name) override{
        readOptions();
        if(name == ControlCharacter::FIXON){
            return options.c_cc[VSTART];
        } else if(name == ControlCharacter::FIXOFF){
            return options.c_cc[VSTOP];
        }
        return 0;
    }
    
    virtual void setSignal(Signal signalCode, bool value) override{
        if(signalCode == Signal::TXD){
            ioctl(fd, value?TIOCSBRK:TIOCCBRK);
        } else if (signalCode == Signal::DTR){
            ioctl(fd, value?TIOCSDTR:TIOCCDTR);
        } else if (signalCode == Signal::RTS){
            int status;
            ioctl(fd, TIOCMGET, &status);
            status = value?status|TIOCM_RTS:status&~TIOCM_RTS;
            ioctl(fd, TIOCMSET, &status);
        } else {
            throw InvalidSignalException();
        }
    }
    
    virtual bool signal(Signal signalCode) override{
        int status;
        ioctl(fd, TIOCMGET, &status);
        if(signalCode == Signal::CTS){
            return status&TIOCM_CTS;
        } else if (signalCode == Signal::DSR){
            return status&TIOCM_DSR;
        } else if (signalCode == Signal::DTR){
            return status&TIOCM_DTR;
        } else if (signalCode == Signal::RTS){
            return status&TIOCM_RTS;
        } else if (signalCode == Signal::DCD){
            return status&TIOCM_CAR;
        } else {
            throw InvalidSignalException();
        }
        return false;
    }
    
    virtual int read(uint8_t *buffer, int size) override{
        fd_set descr;
        timeval tm;
        long seconds = readTimeout()/1000;
        long miliseconds = readTimeout()%1000;
        tm.tv_sec = seconds;
        tm.tv_usec = static_cast<decltype(tm.tv_usec)>(miliseconds*1000);
        FD_ZERO(&descr);
        FD_SET(fd, &descr);
        int retCode = select(fd + 1, &descr, 0, 0, &tm);
        if(retCode == 1){
            return ::read(fd, buffer, (size_t)size);
        }
        return 0;
    }
    
    virtual int write(uint8_t *buffer, int size) override{
        return ::write(fd, buffer, (size_t)size);
    }
    
private:
    void readOptions(){
        tcgetattr(fd, &options);
    }
    
    void applyChanges(){
        tcsetattr(fd, TCSANOW, &options);
    }
    
};

void BasicSerial::init(const char *name){
    this->impl = new NixSerial(name);
}

#endif