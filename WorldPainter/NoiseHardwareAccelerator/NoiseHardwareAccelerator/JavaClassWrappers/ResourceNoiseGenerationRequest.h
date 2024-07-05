#pragma once

#include "NoiseGenerationRequest.h"

#include <jni.h>

class ResourceNoiseGenerationRequest : public NoiseGenerationRequest {
private:
	jmethodID getChancesMethod;
	jmethodID getSeedMethod;

public:
	ResourceNoiseGenerationRequest(JNIEnv* env, jobject resourceNoiseGenerationRequestObject);

	float* getChances();
	long getSeed();
};