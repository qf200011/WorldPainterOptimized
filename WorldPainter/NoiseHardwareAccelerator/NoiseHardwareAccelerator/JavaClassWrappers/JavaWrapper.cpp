#include "JavaWrapper.h"

JavaWrapper::JavaWrapper(JNIEnv* env, jobject javaObject, std::string javaClassString) {
	this->env = env;
	this->javaObject = javaObject;
	this->javaClass = env->FindClass(javaClassString.c_str());
}