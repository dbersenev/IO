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

#ifndef TEST_RUN
#include <stdio.h>
#include <jni.h>

#include "serial.h"

extern "C" {
    jint JNI_OnLoad(JavaVM *vm, void *reserved);
    void JNI_OnUnload(JavaVM *vm, void *reserved);
    
    Serial* fromObject(JNIEnv* env, jobject self);
    unsigned char * directAddress(JNIEnv *env, jobject buffer);
    bool isDirectBuffer(JNIEnv *env, jobject buffer);
    jbyteArray toArray(JNIEnv* env, jobject buffer);
    
    static jclass mainClazz;
    static jmethodID portHnd;
    
    static jclass bufferClass;
    static jmethodID isDirect;
    static jmethodID arrayMethod;
    
    jint JNI_OnLoad(JavaVM *vm, void *reserved){
        JNIEnv *env;
        vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8);
        jclass clazz = env->FindClass("org/molasdin/io/serial/BasicSerial");
        mainClazz = static_cast<jclass>(env->NewGlobalRef(clazz));
        portHnd = env->GetMethodID(mainClazz, "portHnd", "()J");
        
        clazz = env->FindClass("java/nio/ByteBuffer");
        bufferClass = static_cast<jclass>(env->NewGlobalRef(clazz));
        isDirect = env->GetMethodID(bufferClass, "isDirect", "()Z");
        arrayMethod = env->GetMethodID(bufferClass, "array", "()[B");
        return JNI_VERSION_1_8;
    }
    
    void JNI_OnUnload(JavaVM *vm, void *reserved){
        JNIEnv *env;
        vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
        env->DeleteGlobalRef(mainClazz);
        env->DeleteGlobalRef(bufferClass);
    }
    
    JNIEXPORT jlong JNICALL Java_org_molasdin_io_serial_BasicSerial_openPort(JNIEnv *env, jobject self, jstring str){
        jboolean isCopy = false;
        const char* value = env->GetStringUTFChars(str, &isCopy);
        Serial *serial = nullptr;
        try {
            serial = new BasicSerial(value);
            env->ReleaseStringUTFChars(str, value);
            serial->open();
            return reinterpret_cast<jlong>(serial);
        } catch (CanNotOpenPortException &ex) {
            if(serial != nullptr){
                delete serial;
            }
            jclass exClass = env->FindClass("java/lang/Exception");
            env->ThrowNew(exClass, "Can not open port");
        }
        return 0;
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_closePort(JNIEnv *env, jobject self){
        Serial* serial = fromObject(env, self);
        delete serial;
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortReadTimeout(JNIEnv *env, jobject self, jlong timeout){
        Serial *serial = fromObject(env, self);
        serial->setReadTimeout(static_cast<long>(timeout));
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortBaud(JNIEnv *env, jobject self, jint baud){
        Serial* serial = fromObject(env, self);
        serial->setBaudRate(static_cast<int>(baud));
    }
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_portBaud(JNIEnv *env, jobject self, jint bits){
        Serial* serial = fromObject(env, self);
        return static_cast<jint>(serial->baudRate());
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortBits(JNIEnv *env, jobject self, jint bits){
        Serial* serial = fromObject(env, self);
        serial->setDataBits(static_cast<Serial::DataBits>(bits));
    }
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_portBits(JNIEnv *env, jobject self){
        Serial* serial = fromObject(env, self);
        return static_cast<jint>(serial->dataBits());
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortStopBits(JNIEnv *env, jobject self, jint bits){
        Serial* serial = fromObject(env, self);
        serial->setStopBits(static_cast<Serial::StopBits>(bits));
    }
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_portStopBits(JNIEnv *env, jobject self){
        Serial* serial = fromObject(env, self);
        return static_cast<jint>(serial->stopBits());
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortParity(JNIEnv *env, jobject self, jint parity){
        Serial* serial = fromObject(env, self);
        serial->setParity(static_cast<Serial::Parity>(parity));
    }
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_portParity(JNIEnv *env, jobject self){
        Serial* serial = fromObject(env, self);
        return static_cast<jint>(serial->parity());
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortFlowCtl(JNIEnv *env, jobject self, jint ctl){
        Serial* serial = fromObject(env, self);
        serial->setFlowControl(static_cast<Serial::FlowControl>(ctl));
    }
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_portFlowCtl(JNIEnv *env, jobject self){
        Serial* serial = fromObject(env, self);
        return static_cast<jint>(serial->flowControl());
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPortChar(JNIEnv *env, jobject self, jint id, jbyte value){
        Serial* serial = fromObject(env, self);
        serial->setControlCharacter(static_cast<Serial::ControlCharacter>(id), static_cast<uint8_t>(value));
    }
    JNIEXPORT jbyte JNICALL Java_org_molasdin_io_serial_BasicSerial_portChar(JNIEnv *env, jobject self, jint id){
        Serial* serial = fromObject(env, self);
        return static_cast<jbyte>(serial->controlCharacter(static_cast<Serial::ControlCharacter>(id)));
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_setPinSignal(JNIEnv *env, jobject self, jint signal, jboolean value){
        Serial* serial = fromObject(env, self);
        serial->setSignal(static_cast<Serial::Signal>(signal), value);
    }
    JNIEXPORT jboolean JNICALL Java_org_molasdin_io_serial_BasicSerial_pinSignal(JNIEnv *env, jobject self, jint signal){
        Serial* serial = fromObject(env, self);
        return serial->signal(static_cast<Serial::Signal>(signal));
    }
    
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_read(JNIEnv *env, jobject self, jobject data, jint size){
        Serial* serial = fromObject(env, self);
        jint result = 0;
        if(isDirectBuffer(env, data)){
            result = serial->read(directAddress(env, data), static_cast<int>(size));
        } else {
            jbyteArray tmpArray = toArray(env, data);
            jbyte *array = env->GetByteArrayElements(tmpArray, 0);
            result = serial->read(reinterpret_cast<uint8_t*>(array), static_cast<int>(size));
            env->ReleaseByteArrayElements(static_cast<jbyteArray>(tmpArray), array, 0);
        }
        return result;
    }
    
    JNIEXPORT jint JNICALL Java_org_molasdin_io_serial_BasicSerial_write(JNIEnv *env, jobject self, jobject data, jint size){
        Serial* serial = fromObject(env, self);
        jint result = 0;
        if(isDirectBuffer(env, data)){
            result = serial->write(directAddress(env, data), static_cast<int>(size));
        } else {
            jbyteArray tmpArray = toArray(env, data);
            jbyte *array = env->GetByteArrayElements(tmpArray, 0);
            result = serial->write(reinterpret_cast<uint8_t*>(array), static_cast<int>(size));
            env->ReleaseByteArrayElements(static_cast<jbyteArray>(tmpArray), array, 0);
        }
        return result;
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_prepareToCheckSignals(JNIEnv *env, jobject self, jobject signals){
        Serial* serial = fromObject(env, self);
        jint *signalsArray = env->GetIntArrayElements(static_cast<jintArray>(signals), 0);
        serial->prepareToCheckSignals(reinterpret_cast<Serial::Signal*>(signalsArray),
            static_cast<uint16_t>(env->GetArrayLength(static_cast<jarray>(signals))));
        env->ReleaseIntArrayElements(static_cast<jintArray>(signals), signalsArray, JNI_ABORT);
    }
    
    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_changedSignals(JNIEnv *env, jobject self, jobject changedSignals){
        Serial* serial = fromObject(env, self);
        if(!isDirectBuffer(env, changedSignals)){
          jclass exClass = env->FindClass("java/lang/Exception");
          env->ThrowNew(exClass, "Signals buffer must be direct");
          return;
        }
        serial->detectChanged(directAddress(env, changedSignals));
    }

    JNIEXPORT void JNICALL Java_org_molasdin_io_serial_BasicSerial_terminateWaitForSignals(JNIEnv *env, jobject self) {
        Serial* serial = fromObject(env, self);
        serial->terminateSignalsWait();
    }
    
    Serial* fromObject(JNIEnv* env, jobject self){
        jlong ref = env->CallLongMethod(self, portHnd);
        return reinterpret_cast<Serial*>(ref);
    }
    
    uint8_t * directAddress(JNIEnv *env, jobject buffer){
        return static_cast<uint8_t *>(env->GetDirectBufferAddress(buffer));
    }
    bool isDirectBuffer(JNIEnv *env, jobject buffer){
        return env->CallBooleanMethod(buffer, isDirect);
    }
    
    jbyteArray toArray(JNIEnv* env, jobject buffer){
        return static_cast<jbyteArray>(env->CallObjectMethod(buffer, arrayMethod));
    }
}
#endif




