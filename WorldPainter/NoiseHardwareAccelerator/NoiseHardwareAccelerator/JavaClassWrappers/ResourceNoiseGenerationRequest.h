#pragma once

#include "NoiseGenerationRequest.h"

#include <jni.h>

class ResourceNoiseGenerationRequest : public NoiseGenerationRequest {
private:
	static jmethodID getChancesMethod;
	static jmethodID getSeedMethod;

public:
	ResourceNoiseGenerationRequest(JNIEnv* env);

	float* getChances();
	long getSeed();
};