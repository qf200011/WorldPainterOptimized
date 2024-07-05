#include "NoiseGenerationRequest.h"

NoiseGenerationRequest::NoiseGenerationRequest(JNIEnv* env) {
	if (noiseGenerationRequest == NULL) {
		noiseGenerationRequest = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/NoiseGenerationRequest");

		getRegionXMethod = env->GetMethodID(noiseGenerationRequest, "getRegionX", "()I");
		getRegionYMethod = env->GetMethodID(noiseGenerationRequest, "getRegionY", "()I");
		getMaterialMinHeightMethod = env->GetMethodID(noiseGenerationRequest, "getMinHeight", "()I");
		getMaterialMaxHeightMethod = env->GetMethodID(noiseGenerationRequest, "getMaxHeight", "()I");
		getBlobSizeMethod = env->GetMethodID(noiseGenerationRequest, "getBlobSize", "()F");
	}
}

int NoiseGenerationRequest::getRegionX() {
	return env->CallIntMethod(noiseGenerationRequest, getRegionXMethod);
}

int NoiseGenerationRequest::getRegionY() {
	return env->CallIntMethod(noiseGenerationRequest, getRegionYMethod);
}

int NoiseGenerationRequest::getMaterialMinHeight() {
	return env->CallIntMethod(noiseGenerationRequest, getMaterialMinHeightMethod);
}

int NoiseGenerationRequest::getMaterialMaxHeight() {
	return env->CallIntMethod(noiseGenerationRequest, getMaterialMaxHeightMethod);
} 

int NoiseGenerationRequest::getBlobSize() {
	return env->CallFloatMethod(noiseGenerationRequest, getBlobSizeMethod);
} 
