#include "ResourceNoiseGenerationRequest.h"

ResourceNoiseGenerationRequest::ResourceNoiseGenerationRequest(JNIEnv* env) : NoiseGenerationRequest(env){
	noiseRequestClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/ResourceNoiseGenerationRequest");

	getChancesMethod= env->GetMethodID(noiseRequestClass, "getChances", "()[F");
	getSeedMethod = env->GetMethodID(noiseRequestClass, "getSeed", "()J");
}

float* ResourceNoiseGenerationRequest::getChances() {
	jfloatArray chancesArray= (jfloatArray) env->CallObjectMethod(noiseRequestClass, getChancesMethod);
	return env->GetFloatArrayElements(chancesArray, 0);
}

long ResourceNoiseGenerationRequest::getSeed() {
	return env->CallLongMethod(noiseRequestClass, getSeedMethod);
}