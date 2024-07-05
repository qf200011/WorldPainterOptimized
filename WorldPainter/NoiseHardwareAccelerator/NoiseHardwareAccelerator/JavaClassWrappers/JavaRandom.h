#pragma once

#include "JavaWrapper.h"

class JavaRandom : JavaWrapper {
private:
	jmethodID randomSeededConstructorMethod;
	jmethodID nextIntMethod;

public:
	JavaRandom(JNIEnv* env, long seed);

	int nextInt(int bound);
};