#include "GPUNoiseRequest.h"
#include "ResourceNoiseGenerationRequest.h"

GPUNoiseRequest::GPUNoiseRequest(JNIEnv* env, jobject gpuNoiseRequestObject) : JavaWrapper(env, gpuNoiseRequestObject, "org/pepsoft/worldpainter/exporting/NoiseHardwareAccelerator$GPUNoiseRequest")  {
	this->env = env;

	getNoiseGenerationRequestMethod = env->GetMethodID(javaClass, "getNoiseGenerationRequest", "()Lorg/pepsoft/worldpainter/exporting/gpuacceleration/NoiseGenerationRequest;");
	getGPUMemoryBlockMethod = env->GetMethodID(javaClass, "getGpuMemoryBlock", "()Lorg/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock;");
}

ResourceNoiseGenerationRequest GPUNoiseRequest::getResourcesNoiseGenerationRequest() {
	jobject noiseGenerationRequestObject = env->CallObjectMethod(javaObject, getNoiseGenerationRequestMethod);
	return ResourceNoiseGenerationRequest(env, noiseGenerationRequestObject);
}

GPUMemoryBlock GPUNoiseRequest::getGPUMemoryBlock() {
	jobject gpuMemoryObject = env->CallObjectMethod(javaObject, getGPUMemoryBlockMethod);
	return GPUMemoryBlock(env, gpuMemoryObject);
}