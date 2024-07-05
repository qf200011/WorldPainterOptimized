#pragma once

#include <jni.h>

#include "ResourceNoiseGenerationRequest.h"
#include "GPUMemoryBlock.h"
#include "JavaWrapper.h"

class GPUNoiseRequest : JavaWrapper {
private:

	jmethodID getNoiseGenerationRequestMethod;
	jmethodID getGPUMemoryBlockMethod;


public:
	GPUNoiseRequest(JNIEnv* env, jobject gpuNoiseRequestObject);

	ResourceNoiseGenerationRequest getResourcesNoiseGenerationRequest();
	GPUMemoryBlock getGPUMemoryBlock();
};