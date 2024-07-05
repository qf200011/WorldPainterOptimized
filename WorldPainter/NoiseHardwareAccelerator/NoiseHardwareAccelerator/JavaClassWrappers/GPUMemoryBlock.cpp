#include "GPUMemoryBlock.h"

GPUMemoryBlock::GPUMemoryBlock(JNIEnv* env) {
    this->env = env;

	gpuMemoryBlockClass = env ->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock");

    getxGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getxGPUPointer", "()J");
    getyGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getyGPUPointer", "()J");
    getzGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getzGPUPointer", "()J");
    getpGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getpGPUPointer", "()J");
    getOutputGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getOutputGPUPointer", "()J");
    getCompactedOutputGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getCompactedOutputGPUPointer", "()J");
}

long GPUMemoryBlock::getxGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getxGPUPointerMethod);
}

long GPUMemoryBlock::getyGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getyGPUPointerMethod);
}

long GPUMemoryBlock::getzGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getzGPUPointerMethod);
}

long GPUMemoryBlock::getpGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getpGPUPointerMethod);
}

long GPUMemoryBlock::getOutputGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getOutputGPUPointerMethod);
}

long GPUMemoryBlock::getcompactedOutputGPUPointer() {
    return env->CallLongMethod(gpuMemoryBlockClass, getCompactedOutputGPUPointerMethod);
}