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

    long getxGPUPointer();
    long getyGPUPointer();
    long getzGPUPointer();
    long getpGPUPointer();
    long getOutputGPUPointer();
    long getcompactedOutputGPUPointer();
};