#include "NoiseGenerationRequest.h"



NoiseGenerationRequest::NoiseGenerationRequest(JNIEnv* env, jobject noiseGenerationRequestObject,std::string javaClassString) : JavaWrapper(env, noiseGenerationRequestObject,javaClassString) {
	getRegionXMethod = env->GetMethodID(javaClass, "getRegionX", "()I");
	getRegionYMethod = env->GetMethodID(javaClass, "getRegionY", "()I");
	getMinHeightMethod = env->GetMethodID(javaClass, "getMinHeight", "()I");
	getMaxHeightMethod = env->GetMethodID(javaClass, "getMaxHeight", "()I");
	getBlobSizeMethod = env->GetMethodID(javaClass, "getBlobSize", "()F");
}

int NoiseGenerationRequest::getRegionX() {
	return env->CallIntMethod(javaObject, getRegionXMethod);
}

int NoiseGenerationRequest::getRegionY() {
	return env->CallIntMethod(javaObject, getRegionYMethod);
}

int NoiseGenerationRequest::getMinHeight() {
	return env->CallIntMethod(javaObject, getMinHeightMethod);
}

int NoiseGenerationRequest::getMaxHeight() {
	return env->CallIntMethod(javaObject, getMaxHeightMethod);
} 

float NoiseGenerationRequest::getBlobSize() {
	return env->CallFloatMethod(javaObject, getBlobSizeMethod);
} 
