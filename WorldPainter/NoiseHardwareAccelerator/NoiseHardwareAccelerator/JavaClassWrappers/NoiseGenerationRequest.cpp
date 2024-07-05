#include "NoiseGenerationRequest.h"

NoiseGenerationRequest::NoiseGenerationRequest(JNIEnv* env) {
	this->env = env;

	noiseRequestClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/NoiseGenerationRequest");

	getRegionXMethod = env->GetMethodID(noiseRequestClass, "getRegionX", "()I");
	getRegionYMethod = env->GetMethodID(noiseRequestClass, "getRegionY", "()I");
	getMaterialMinHeightMethod = env->GetMethodID(noiseRequestClass, "getMinHeight", "()I");
	getMaterialMaxHeightMethod = env->GetMethodID(noiseRequestClass, "getMaxHeight", "()I");
	getBlobSizeMethod = env->GetMethodID(noiseRequestClass, "getBlobSize", "()F");
}

int NoiseGenerationRequest::getRegionX() {
	return env->CallIntMethod(noiseRequestClass, getRegionXMethod);
}

int NoiseGenerationRequest::getRegionY() {
	return env->CallIntMethod(noiseRequestClass, getRegionYMethod);
}

int NoiseGenerationRequest::getMaterialMinHeight() {
	return env->CallIntMethod(noiseRequestClass, getMaterialMinHeightMethod);
}

int NoiseGenerationRequest::getMaterialMaxHeight() {
	return env->CallIntMethod(noiseRequestClass, getMaterialMaxHeightMethod);
} 

int NoiseGenerationRequest::getBlobSize() {
	return env->CallFloatMethod(noiseRequestClass, getBlobSizeMethod);
} 
