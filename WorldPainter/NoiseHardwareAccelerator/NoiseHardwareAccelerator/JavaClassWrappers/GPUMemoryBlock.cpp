#include "GPUMemoryBlock.h"

GPUMemoryBlock::GPUMemoryBlock(JNIEnv* env, jobject gpuMemoryBlockObject) : JavaWrapper(env, gpuMemoryBlockObject, "org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock") {

    getxGPUPointerMethod = env->GetMethodID(javaClass, "getxGPUPointer", "()J");
    getyGPUPointerMethod = env->GetMethodID(javaClass, "getyGPUPointer", "()J");
    getzGPUPointerMethod = env->GetMethodID(javaClass, "getzGPUPointer", "()J");
    getpGPUPointerMethod = env->GetMethodID(javaClass, "getpGPUPointer", "()J");
    getOutputGPUPointerMethod = env->GetMethodID(javaClass, "getOutputGPUPointer", "()J");
    getCompactedOutputGPUPointerMethod = env->GetMethodID(javaClass, "getCompactedOutputGPUPointer", "()J");
}

long long GPUMemoryBlock::getxGPUPointer() {
    return env->CallLongMethod(javaObject, getxGPUPointerMethod);
}

long long GPUMemoryBlock::getyGPUPointer() {
    return env->CallLongMethod(javaObject, getyGPUPointerMethod);
}

long long GPUMemoryBlock::getzGPUPointer() {
    return env->CallLongMethod(javaObject, getzGPUPointerMethod);
}

long long GPUMemoryBlock::getpGPUPointer() {
    return env->CallLongMethod(javaObject, getpGPUPointerMethod);
}

long long GPUMemoryBlock::getOutputGPUPointer() {
    return env->CallLongMethod(javaObject, getOutputGPUPointerMethod);
}

long long GPUMemoryBlock::getcompactedOutputGPUPointer() {
    return env->CallLongMethod(javaObject, getCompactedOutputGPUPointerMethod);
}