#pragma once

#include <jni.h>

#include "JavaWrapper.h"

class NoiseGenerationRequest : protected JavaWrapper {
private:
	jmethodID getRegionXMethod;
	jmethodID getRegionYMethod;
	jmethodID getMinHeightMethod;
	jmethodID getMaxHeightMethod;
	jmethodID getBlobSizeMethod;

protected:
	NoiseGenerationRequest(JNIEnv* env, jobject noiseGenerationRequestObject, std::string javaClassString);

public:
	static int HEIGHT_SIZE;

	int getRegionX();
	int getRegionY();
	int getMinHeight();
	int getMaxHeight();
	float getBlobSize();
};

