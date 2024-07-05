#pragma once

#include <jni.h>

class NoiseGenerationRequest {
private:
	JNIEnv* env;

private:
	static jclass noiseGenerationRequest;

	static jmethodID getRegionXMethod;
	static jmethodID getRegionYMethod;
	static jmethodID getMaterialMinHeightMethod;
	static jmethodID getMaterialMaxHeightMethod;
	static jmethodID getBlobSizeMethod;

public:
	static int HEIGHT_SIZE;

	NoiseGenerationRequest(JNIEnv* env);

	int getRegionX();
	int getRegionY();
	int getMaterialMinHeight();
	int getMaterialMaxHeight();
	int getBlobSize();
};

