#include "NoiseGenerationRequest.h"



NoiseGenerationRequest::NoiseGenerationRequest(JNIEnv* env, jobject noiseGenerationRequestObject,std::string javaClassString) : JavaWrapper(env, noiseGenerationRequestObject,javaClassString) {
	getRegionXMethod = env->GetMethodID(javaClass, "getRegionX", "()I");
	getRegionYMethod = env->GetMethodID(javaClass, "getRegionY", "()I");
	getMaterialMinHeightMethod = env->GetMethodID(javaClass, "getMinHeight", "()I");
	getMaterialMaxHeightMethod = env->GetMethodID(javaClass, "getMaxHeight", "()I");
	getBlobSizeMethod = env->GetMethodID(javaClass, "getBlobSize", "()F");
}

int NoiseGenerationRequest::getRegionX() {
	return env->CallIntMethod(javaObject, getRegionXMethod);
}

int NoiseGenerationRequest::getRegionY() {
	return env->CallIntMethod(javaObject, getRegionYMethod);
}

int NoiseGenerationRequest::getMaterialMinHeight() {
	return env->CallIntMethod(javaObject, getMaterialMinHeightMethod);
}

int NoiseGenerationRequest::getMaterialMaxHeight() {
	return env->CallIntMethod(javaObject, getMaterialMaxHeightMethod);
} 

int NoiseGenerationRequest::getBlobSize() {
	return env->CallFloatMethod(javaObject, getBlobSizeMethod);
} 
