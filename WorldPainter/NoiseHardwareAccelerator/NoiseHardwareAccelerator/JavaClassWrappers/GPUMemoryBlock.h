#pragma once

#include <jni.h>

class GPUMemoryBlock {
private:
    JNIEnv* env;

    jclass gpuMemoryBlockClass;

    jmethodID getxGPUPointerMethod;
    jmethodID getyGPUPointerMethod;
    jmethodID getzGPUPointerMethod;
    jmethodID getpGPUPointerMethod;
    jmethodID getOutputGPUPointerMethod;
    jmethodID getCompactedOutputGPUPointerMethod;

public:
	GPUMemoryBlock(JNIEnv* env);

    long getxGPUPointer();
    long getyGPUPointer();
    long getzGPUPointer();
    long getpGPUPointer();
    long getOutputGPUPointer();
    long getcompactedOutputGPUPointer();
};