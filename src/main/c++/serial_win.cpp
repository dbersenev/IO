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

#include "serial.h"

#ifdef SERIAL_WIN
#include <stdio.h>
#include <windows.h>
#include <winbase.h>

#include "serial.h"


class WinSerial:public Serial{
private:
    HANDLE fd;
    DCB dcb = {0};
    OVERLAPPED io;
public:
    WinSerial(const char *name):Serial(name){
    }

    virtual ~WinSerial() {
        if(isOpen()){
            terminateSignalsWait();
            close();
        }
    }

    virtual void open() override{
        fd = ::CreateFile( name(), GENERIC_READ|GENERIC_WRITE, 0, 0, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, 0);
        if(fd == INVALID_HANDLE_VALUE){
            fd = 0;
            throw CanNotOpenPortException();
        }
        readOptions();
        dcb.fDtrControl = DTR_CONTROL_DISABLE;
        dcb.fRtsControl = RTS_CONTROL_DISABLE;
        applyChanges();
        setBaudRate(110);
        setReadTimeout(readTimeout());
    }

    virtual void close() override{
        ::CloseHandle(fd);
        fd = 0;
    }

    virtual bool isOpen() override{
        return fd != 0;
    }

    virtual void setBaudRate(int baudRate) override{
        DWORD tmp = CBR_110;
        switch (baudRate) {
            case 110:
                tmp = CBR_110;
                break;
            case 300:
                tmp = CBR_300;
                break;
            case 600:
                tmp = CBR_600;
                break;
            case 1200:
                tmp = CBR_1200;
                break;
            case 2400:
                tmp = CBR_2400;
                break;
            case 4800:
                tmp = CBR_4800;
                break;
            case 9600:
                tmp = CBR_9600;
                break;
            case 14400:
                tmp = CBR_14400;
                break;
            case 19200:
                tmp = CBR_19200;
                break;
            case 38400:
                tmp = CBR_38400;
                break;
            case 56000:
                tmp = CBR_56000;
                break;
            case 128000:
                tmp = CBR_128000;
                break;
            case 256000:
                tmp = CBR_256000;
                break;
        }
        dcb.BaudRate = tmp;
        applyChanges();
    }
    virtual int baudRate() override{
        DWORD baudRate = dcb.BaudRate;
        int tmp = 0;
        switch (baudRate) {
            case CBR_110:
                tmp = 110;
                break;
            case CBR_300:
                tmp = 300;
                break;
            case CBR_600:
                tmp = 600;
                break;
            case CBR_1200:
                tmp = 1200;
                break;
            case CBR_2400:
                tmp = 2400;
                break;
            case CBR_4800:
                tmp = 4800;
                break;
            case CBR_9600:
                tmp = 9600;
                break;
            case CBR_14400:
                tmp = 14400;
                break;
            case CBR_19200:
                tmp = 19200;
                break;
            case CBR_38400:
                tmp = 38400;
                break;
            case CBR_56000:
                tmp = 56000;
                break;
            case CBR_128000:
                tmp = 128000;
                break;
            case CBR_256000:
                tmp = 256000;
                break;
        }
        return tmp;
    }

    virtual void setDataBits(Serial::DataBits dataBits) override{
        BYTE flag = 0;
        switch (dataBits) {
            case DataBits::DBCS5:
                flag = 5;
                break;
            case DataBits::DBCS6:
                flag = 6;
                break;
            case DataBits::DBCS7:
                flag = 7;
                break;
            case DataBits::DBCS8:
                flag = 8;
        }
        dcb.ByteSize = flag;
        applyChanges();
    }

    virtual DataBits dataBits() override{
        if(dcb.ByteSize == 8){
            return DataBits::DBCS8;
        } else if(dcb.ByteSize == 7){
            return DataBits::DBCS7;
        } else if(dcb.ByteSize == 6){
            return DataBits::DBCS6;
        } else {
            return DataBits::DBCS5;
        }
    }

    virtual void setStopBits(StopBits bits) override{
        if (bits == StopBits::ONE){
            dcb.StopBits = ONESTOPBIT;
        } else{
            dcb.StopBits = TWOSTOPBITS;
        }
        applyChanges();
    }
    virtual StopBits stopBits() override{
        if (dcb.StopBits == TWOSTOPBITS){
            return StopBits::TWO;
        }
        return StopBits::ONE;
    }

    virtual void setParity(Parity parity) override{
        BYTE tmp = NOPARITY;
        switch (parity) {
            case Parity::EVEN:
                tmp = EVENPARITY;
                break;
            case Parity::ODD:
                tmp = ODDPARITY;
                break;
            default:
                tmp = NOPARITY;
                break;
        }
        dcb.Parity = tmp;
        applyChanges();
    }
    virtual Parity parity() override{
        if (dcb.Parity == ODDPARITY){
            return Parity::ODD;
        } else if (dcb.Parity == EVENPARITY){
            return Parity::EVEN;
        } else {
            return Parity::DISABLED;
        }
    }

    virtual void setFlowControl(FlowControl flowControl) override{
        dcb.fDtrControl = DTR_CONTROL_DISABLE;
        dcb.fRtsControl = DTR_CONTROL_DISABLE;
        dcb.fOutxCtsFlow = FALSE;
        dcb.fOutxDsrFlow = FALSE;
        dcb.fOutX = FALSE;
        dcb.fInX = FALSE;
        switch (flowControl) {
            case FlowControl::HARDWARE:
                dcb.fDtrControl = DTR_CONTROL_HANDSHAKE;
                dcb.fOutxCtsFlow = TRUE;
                break;
            case FlowControl::SOFTWARE_XON:
                dcb.fOutX = TRUE;
                break;
            case FlowControl::SOFTWARE_XOFF:
                dcb.fInX = TRUE;
                break;
            case FlowControl::SOFTWARE_BOTH:
                dcb.fInX = TRUE;
                dcb.fOutX = TRUE;
        }
        applyChanges();

    }
    virtual FlowControl flowControl() override{
        if(dcb.fDtrControl != DTR_CONTROL_HANDSHAKE &&
                dcb.fOutxCtsFlow == FALSE &&
                dcb.fOutX == FALSE &&
                dcb.fInX == FALSE){
            return FlowControl::NONE;
        } else if(dcb.fDtrControl == DTR_CONTROL_HANDSHAKE){
            return FlowControl::HARDWARE;
        } else if(dcb.fOutX == TRUE && dcb.fInX == FALSE){
            return FlowControl::SOFTWARE_XON;
        } else if(dcb.fInX == TRUE && dcb.fOutX == FALSE){
            return FlowControl::SOFTWARE_XOFF;
        } else {
            return FlowControl::SOFTWARE_BOTH;
        }
    }

    virtual void setControlCharacter(ControlCharacter name, uint8_t value) override{
        if(name == ControlCharacter::FIXON){
            dcb.XonChar = value;
        } else if (name == ControlCharacter::FIXOFF){
            dcb.XoffChar = value;
        }
        applyChanges();
    }
    virtual uint8_t controlCharacter(ControlCharacter name) override{
        readOptions();
        if(name == ControlCharacter::FIXON){
            return dcb.XonChar;
        } else if(name == ControlCharacter::FIXOFF){
            return dcb.XoffChar;
        }
        return 0;
    }

    virtual void setSignal(Signal signalCode, bool value) override{
        DWORD func = 0;
        if(signalCode == Signal::TXD){
            if(value){
                SetCommBreak(fd);
            } else{
                ClearCommBreak(fd);
            }
            return;
        } else if (signalCode == Signal::DTR){
            func = value?SETDTR:CLRDTR;
        } else if (signalCode == Signal::RTS){
            func = value?SETRTS:CLRRTS;
        } else {
            throw InvalidSignalException();
        }
        EscapeCommFunction(fd, func);
    }

    virtual void setReadTimeout(long timeout) override{
        Serial::setReadTimeout(timeout);
        COMMTIMEOUTS tm = {0};
        tm.ReadIntervalTimeout = MAXDWORD;
        tm.ReadTotalTimeoutMultiplier = MAXDWORD;
        tm.ReadTotalTimeoutConstant = static_cast<DWORD>(timeout);
        SetCommTimeouts(fd, &tm);
    }

    virtual bool signal(Signal signalCode) override{
        DWORD status = 0;
        GetCommModemStatus(fd, &status);
        if(signalCode == Signal::CTS){
            return (status&MS_CTS_ON) != 0;
        } else if (signalCode == Signal::DSR){
            return (status&MS_DSR_ON) != 0;
        } else if (signalCode == Signal::DCD){
            return (status&MS_RLSD_ON) != 0;
        } else {
            throw InvalidSignalException();
        }
    }

    virtual void detectChanged(uint8_t changed[]) override{
        cleanChanged(changed);
        DWORD result = 0;
        DWORD tmp = 0;
        if(WaitCommEvent(fd, &result, &io) == 0){
            if(GetLastError() != ERROR_IO_PENDING){
                throw SerialException();
            }
            if(GetOverlappedResult(fd, &io, &tmp, TRUE) == 0){
                throw SerialException();
            }
        }

        if(result == 0){
            return;
        }
        if(result & EV_CTS){
            addChanged(changed, Signal::CTS, signal(Signal::CTS));
        }
        if (result & EV_DSR){
            addChanged(changed, Signal::DSR, signal(Signal::DSR));
        }
        if (result & EV_RLSD){
            addChanged(changed, Signal::DCD, signal(Signal::DCD));
        }
    }

    virtual void terminateSignalsWait() override{
        SetCommMask(fd, 0);
    }

    virtual void prepareToCheckSignals(Signal signals[], int size) override{
        DWORD mask = 0;
        for(int i = 0; i < size; i++){
            switch(signals[i]){
                case Signal::CTS:
                    mask = mask | EV_CTS;
                    break;
                case Signal::DSR:
                    mask = mask | EV_DSR;
                    break;
                case Signal::DCD:
                    mask = mask | EV_RLSD;
                default:
                    throw InvalidSignalException();
            }
        }
        SetCommMask(fd, mask);
        prepareOverlapped();
    }

    virtual int read(uint8_t *buffer, int size) override{
        DWORD bytesRead = 0;
        prepareOverlapped();
        if(ReadFile(fd, buffer, static_cast<DWORD>(size), &bytesRead, &io) == 0){
            if(GetLastError() != ERROR_IO_PENDING){
                throw ReadException();
            }
            if(GetOverlappedResult(fd, &io, &bytesRead, TRUE) == 0){
                throw ReadException();
            }
        }
        return static_cast<int>(bytesRead);
    }

    virtual int write(uint8_t *buffer, int size) override{
        DWORD bytesWritten = 0;
        prepareOverlapped();
        if(WriteFile(fd, buffer, static_cast<DWORD>(size), &bytesWritten, &io) == 0){
            if (GetLastError() != ERROR_IO_PENDING){
                throw WriteException();
            }

            if(GetOverlappedResult(fd, &io, &bytesWritten, TRUE) == 0){
                throw ReadException();
            }
        }
        return static_cast<int>(bytesWritten);
    }

private:
    void readOptions(){
        if(::GetCommState(fd, &dcb) == 0){
            close();
            throw CanNotReadConfigException();
        }
    }

    void applyChanges(){
        if (::SetCommState (fd,&dcb) == 0) {
            close();
            throw CanNotWriteConfigException();
        }
    }

    void prepareOverlapped(){
        io = {0};
        io.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
        if(io.hEvent == 0){
            throw SerialException();
        }
    }
};

void BasicSerial::init(const char *name){
    this->impl = new WinSerial(name);
}

#endif
