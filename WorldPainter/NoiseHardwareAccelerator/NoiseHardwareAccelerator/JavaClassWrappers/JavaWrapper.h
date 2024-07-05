#pragma once

#include <jni.h>
#include <string>

class JavaWrapper {
private:
protected:
	JNIEnv* env;

	jobject javaObject;

	jclass javaClass;
protected:
	JavaWrapper(JNIEnv* env, jobject javaObject, std::string javaClassString);
	JavaWrapper(JNIEnv* env, std::string javaClassString);
};