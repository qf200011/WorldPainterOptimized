#pragma once

#include "JavaWrapper.h"

#include <jni.h>


class GPUMemoryBlock : public JavaWrapper {
private:
    jmethodID getxGPUPointerMethod;
    jmethodID getyGPUPointerMethod;
    jmethodID getzGPUPointerMethod;
    jmethodID getpGPUPointerMethod;
    jmethodID getOutputGPUPointerMethod;
    jmethodID getCompactedOutputGPUPointerMethod;

    jmethodID setxGPUPointerMethod;
    jmethodID setyGPUPointerMethod;
    jmethodID setzGPUPointerMethod;
    jmethodID setpGPUPointerMethod;
    jmethodID setOutputGPUPointerMethod;
    jmethodID setCompactedOutputGPUPointerMethod;

public:
	GPUMemoryBlock(JNIEnv* env, jobject gpuMemoryBlockObject);
    GPUMemoryBlock(JNIEnv* env);

    jobject getJavaObject();

    long long getxGPUPointer();
    long long getyGPUPointer();
    long long getzGPUPointer();
    long long getpGPUPointer();
    long long getOutputGPUPointer();
    long long getCompactedOutputGPUPointer();

    void setxGPUPointer(long long xGPUPointer);
    void setyGPUPointer(long long yGPUPointer);
    void setzGPUPointer(long long zGPUPointer);
    void setpGPUPointer(long long pGPUPointer);
    void setOutputGPUPointer(long long outputGPUPointer);
    void setCompactedOutputGPUPointer(long long compactedOutputGPUPointer);
};