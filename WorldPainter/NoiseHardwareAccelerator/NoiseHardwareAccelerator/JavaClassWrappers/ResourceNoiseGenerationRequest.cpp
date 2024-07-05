#include "ResourceNoiseGenerationRequest.h"

ResourceNoiseGenerationRequest::ResourceNoiseGenerationRequest(JNIEnv* env, jobject resourceNoiseGenerationRequestObject) : NoiseGenerationRequest(env, resourceNoiseGenerationRequestObject,"org/pepsoft/worldpainter/exporting/gpuacceleration/ResourceNoiseGenerationRequest") {
	getChancesMethod= env->GetMethodID(javaClass, "getChances", "()[F");
	getSeedMethod = env->GetMethodID(javaClass, "getSeed", "()J");
}

float* ResourceNoiseGenerationRequest::getChances() {
	jfloatArray chancesArray= (jfloatArray) env->CallObjectMethod(javaObject, getChancesMethod);
	return env->GetFloatArrayElements(chancesArray, 0);
}

long long ResourceNoiseGenerationRequest::getSeed() {
	return env->CallLongMethod(javaObject, getSeedMethod);
}