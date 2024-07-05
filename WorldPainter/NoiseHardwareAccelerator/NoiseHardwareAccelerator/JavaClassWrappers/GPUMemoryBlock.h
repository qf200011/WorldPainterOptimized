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

public:
	GPUMemoryBlock(JNIEnv* env, jobject gpuMemoryBlockObject);

    long long getxGPUPointer();
    long long getyGPUPointer();
    long long getzGPUPointer();
    long long getpGPUPointer();
    long long getOutputGPUPointer();
    long long getcompactedOutputGPUPointer();
};