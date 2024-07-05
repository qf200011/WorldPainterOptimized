#include "GPUMemoryBlock.h"

GPUMemoryBlock::GPUMemoryBlock(JNIEnv* env, jobject gpuMemoryBlockObject) : JavaWrapper(env, gpuMemoryBlockObject, "org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock") {

    getxGPUPointerMethod = env->GetMethodID(javaClass, "getxGPUPointer", "()J");
    getyGPUPointerMethod = env->GetMethodID(javaClass, "getyGPUPointer", "()J");
    getzGPUPointerMethod = env->GetMethodID(javaClass, "getzGPUPointer", "()J");
    getpGPUPointerMethod = env->GetMethodID(javaClass, "getpGPUPointer", "()J");
    getOutputGPUPointerMethod = env->GetMethodID(javaClass, "getOutputGPUPointer", "()J");
    getCompactedOutputGPUPointerMethod = env->GetMethodID(javaClass, "getCompactedOutputGPUPointer", "()J");

    setxGPUPointerMethod = env->GetMethodID(javaClass, "setxGPUPointer", "(J)V");
    setyGPUPointerMethod = env->GetMethodID(javaClass, "setyGPUPointer", "(J)V");;
    setzGPUPointerMethod= env->GetMethodID(javaClass, "setzGPUPointer", "(J)V");;
    setpGPUPointerMethod = env->GetMethodID(javaClass, "setpGPUPointer", "(J)V");
    setOutputGPUPointerMethod = env->GetMethodID(javaClass, "setOutputGPUPointer", "(J)V");
    setCompactedOutputGPUPointerMethod= env->GetMethodID(javaClass, "setCompactedOutputGPUPointer", "(J)V");
}

GPUMemoryBlock::GPUMemoryBlock(JNIEnv* env) : JavaWrapper(env, "org/pepsoft/worldpainter/exporting/NoiseHardwareAccelerator$GPUNoiseRequest") {
    this->env = env;
}

jobject GPUMemoryBlock::getJavaObject() {
    return javaObject;
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

long long GPUMemoryBlock::getCompactedOutputGPUPointer() {
    return env->CallLongMethod(javaObject, getCompactedOutputGPUPointerMethod);
}

void GPUMemoryBlock::setxGPUPointer(long long xGPUPointer) {
    env->CallVoidMethod(javaObject, setxGPUPointerMethod, xGPUPointer);
}

void GPUMemoryBlock::setyGPUPointer(long long yGPUPointer) {
    env->CallVoidMethod(javaObject, setyGPUPointerMethod, yGPUPointer);
}

void GPUMemoryBlock::setzGPUPointer(long long zGPUPointer) {
    env->CallVoidMethod(javaObject, setzGPUPointerMethod, zGPUPointer);
}

void GPUMemoryBlock::setpGPUPointer(long long pGPUPointer) {
    env->CallVoidMethod(javaObject, setpGPUPointerMethod, pGPUPointer);
}

void GPUMemoryBlock::setOutputGPUPointer(long long outputGPUPointer) {
    env->CallVoidMethod(javaObject, setOutputGPUPointerMethod, outputGPUPointer);
}

void GPUMemoryBlock::setCompactedOutputGPUPointer(long long compactedOutputGPUPointer) {
    env->CallVoidMethod(javaObject, setCompactedOutputGPUPointerMethod, compactedOutputGPUPointer);
}