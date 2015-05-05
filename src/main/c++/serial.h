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

#ifndef __SerialInterface__serial__
#define __SerialInterface__serial__

#include <stdint.h>
#include <string.h>
#include <thread>

class SerialException{

};

class InvalidSignalException: public SerialException{
    
};

class CanNotOpenPortException: public SerialException{
    
};

class CanNotReadConfigException: public SerialException{

};

class CanNotWriteConfigException: public SerialException{

};

class ReadException: public SerialException{

};

class WriteException: public SerialException{

};



class Serial{
public:
    enum class DataBits{
        DBCS5 = 5, DBCS6 = 6, DBCS7 = 7, DBCS8 = 8
    };
    
    enum class StopBits{
        ONE = 1, TWO = 2
    };
    
    enum class Parity{
        DISABLED = 0, EVEN = 2, ODD = 1
    };
    
    enum class Signal{
        TXD = 0, RXD = 1, RTS = 2, DTR = 3,
        CTS = 4, DSR = 5, DCD = 6
    };
    
    enum class FlowControl{
        NONE = 0, HARDWARE = 1,
        SOFTWARE_XON = 2, SOFTWARE_XOFF = 3, SOFTWARE_BOTH = 4
    };
    
    enum class ControlCharacter{
      FIXON = 0, FIXOFF = 1
    };
    
private:
    char *deviceName;
    //snapshot with old signal values
    int snapshotSize = 0;
    Signal *snapshot = nullptr;
    bool *snapshotValues= nullptr;
    long timeout = 12000;
    bool isTerminateSigWait = false;
    
public:

    /**
        Constructor is used mostly for BasicSerial
    */
    Serial():deviceName(nullptr){
    }

    Serial(const char *name){
        int nameLength = strlen(name);
        deviceName = new char[nameLength + 1];
        strncpy(deviceName, name, nameLength + 1);
    }
    
    virtual ~Serial(){
        cleanSnapshot();
        if(deviceName != nullptr){
            delete [] deviceName;
        }
    }
    
    virtual const char* name(){
        return deviceName;
    }
    
    virtual void open() = 0;
    virtual void close() = 0;
    virtual bool isOpen() = 0;
    
    virtual void setBaudRate(int baudRate) = 0;

    virtual int baudRate() = 0;
    
    virtual void setDataBits(DataBits dataBits) = 0;
    virtual DataBits dataBits() = 0;
    
    virtual void setStopBits(StopBits bits) = 0;
    virtual StopBits stopBits() = 0;
    
    virtual void setParity(Parity parity) = 0;
    virtual Parity parity() = 0;
    
    virtual void setFlowControl(FlowControl flowControl) = 0;
    virtual FlowControl flowControl() = 0;
    
    virtual void setControlCharacter(ControlCharacter name, uint8_t value) = 0;
    virtual uint8_t controlCharacter(ControlCharacter name) = 0;
    
    virtual void setSignal(Signal signalCode, bool value) = 0;
    
    virtual bool signal(Signal signalCode) = 0;
    
    virtual int read(uint8_t *buffer, int size) = 0;
    virtual int write(uint8_t *buffer, int size) = 0;

    virtual void setReadTimeout(long value){
        timeout = value;
    }

    virtual long readTimeout(){
        return timeout;
    }

    /**
        Determines which signals changed their state
        "changed" has following format:
        0 byte - how many signals
        1 byte - signal 1 id
        2 byte - signal 1 value (0,1)
        etc
    */
    virtual void detectChanged(uint8_t changed[]) {
        cleanChanged(changed);

        bool detected = false;

        while(!isTerminateSigWait && !detected){
            for(int i = 0; i < snapshotSignalsSize();i++){
                Signal sig = snapshotSignals()[i];
                bool current = signal(sig);
                if(current != snaphotSignalsValues()[i]){
                    snaphotSignalsValues()[i] = current;
                    addChanged(changed, sig, current);
                    detected = true;
                }
            }

            if(!detected && !isTerminateSigWait){
                std::this_thread::yield();
            }
        }

        isTerminateSigWait = false;
    }

    /**
        Terminates signals detector
    */
    virtual void terminateSignalsWait(){
        isTerminateSigWait = true;
    }
    
    /**
        Initialise some data before detecting signals
        This method allows to decrease amount of passed arguments
        and increase performance for JVM interactions if used with it
    */
    virtual void prepareToCheckSignals(Signal signals[], int size) {
        cleanSnapshot();
        snapshotSize = size;
        snapshot = new Signal[size];
        snapshotValues = new bool[size];
        for(int i = 0; i < size;i++){
            snapshot[i] = signals[i];
            snapshotValues[i] = signal(signals[i]);
        }
    }
protected:
    
    virtual void cleanChanged(uint8_t changed[]){
        changed[0] = 0;
    }

    /**
        Convenient method to add new signal to "changed"
    */
    virtual void addChanged(uint8_t changed[], Signal signal, bool value){
        int qty = changed[0];
        changed[1 + qty*2] = static_cast<uint8_t>(signal);
        changed[1+ qty*2 + 1] = value;
        changed[0] = changed[0] + 1;
    }
    
    virtual Signal* snapshotSignals(){
        return snapshot;
    }
    
    virtual bool* snaphotSignalsValues(){
        return snapshotValues;
    }
    
    virtual int snapshotSignalsSize(){
        return snapshotSize;
    }
    
    virtual void cleanSnapshot(){
        if(snapshot != 0){
            delete [] snapshot;
            delete [] snapshotValues;
            snapshot = nullptr;
            snapshotValues = nullptr;
        }
        snapshotSize = 0;
    }
    
};


/**
    Forwarder for different implementations
*/
class BasicSerial: public Serial{
private:
    Serial *impl;
    void init(const char *name);
public:
    
    BasicSerial(const char *name){
        init(name);
    }
    
    virtual ~BasicSerial(){
        if(impl->isOpen()){
            impl->close();
        }
        delete impl;
    }
    
    virtual void open() override{
        impl->open();
        
    }
    virtual void close() override{
        impl->close();
        
    }
    virtual bool isOpen() override{
        return impl->isOpen();
    }
    
    virtual void setBaudRate(int baudRate) override{
        impl->setBaudRate(baudRate);
    }
    virtual int baudRate() override{
        return impl->baudRate();
    }
    
    virtual void setDataBits(DataBits dataBits) override{
        impl->setDataBits(dataBits);
    }
    virtual DataBits dataBits() override{
        return impl->dataBits();
    }
    
    virtual void setStopBits(StopBits bits) override{
        impl->setStopBits(bits);
    }
    virtual StopBits stopBits() override{
        return impl->stopBits();
    }
    
    virtual void setParity(Parity parity) override{
        impl->setParity(parity);
    }
    virtual Parity parity(){
        return impl->parity();
    }
    
    virtual void setFlowControl(FlowControl flowControl) override{
        impl->setFlowControl(flowControl);
    }
    virtual FlowControl flowControl() override{
        return impl->flowControl();
    }
    
    virtual void setControlCharacter(ControlCharacter name, uint8_t value) override{
        impl->setControlCharacter(name, value);
    }
    virtual uint8_t controlCharacter(ControlCharacter name) override{
        return impl->controlCharacter(name);
    }
    
    virtual void setSignal(Signal signalCode, bool value) override{
        impl->setSignal(signalCode, value);
    }
    
    virtual bool signal(Signal signalCode) override{
        return impl->signal(signalCode);
    }
    
    virtual int read(uint8_t *buffer, int size) override{
        return impl->read(buffer, size);
    }
    virtual int write(uint8_t *buffer, int size) override{
        return impl->write(buffer, size);
    }
    
    virtual void setReadTimeout(long value) override{
        impl->setReadTimeout(value);
    }

    virtual long readTimeout(){
        return impl->readTimeout();
    }
    virtual void detectChanged(uint8_t changed[]) override{
        impl->detectChanged(changed);
    }
    
    virtual void prepareToCheckSignals(Signal signals[], int size) {
        impl->prepareToCheckSignals(signals, size);
    }

    virtual void terminateSignalsWait(){
        impl->terminateSignalsWait();
    }
    
};

#endif /* defined(__SerialInterface__serial__) */
