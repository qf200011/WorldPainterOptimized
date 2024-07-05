#pragma once

#include <jni.h>

class NoiseGenerationRequest {
private:
	static jmethodID getRegionXMethod;
	static jmethodID getRegionYMethod;
	static jmethodID getMaterialMinHeightMethod;
	static jmethodID getMaterialMaxHeightMethod;
	static jmethodID getBlobSizeMethod;

protected:
	jclass noiseRequestClass;
	JNIEnv* env;

public:
	static int HEIGHT_SIZE;

	NoiseGenerationRequest(JNIEnv* env);

	int getRegionX();
	int getRegionY();
	int getMaterialMinHeight();
	int getMaterialMaxHeight();
	int getBlobSize();
};

