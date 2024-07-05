#include "JavaRandom.h"

JavaRandom::JavaRandom(JNIEnv* env, long seed) : JavaWrapper(env, "java/util/Random") {

	randomSeededConstructorMethod = env->GetMethodID(javaClass, "<init>", "(J)V");
	nextIntMethod = env->GetMethodID(javaClass, "nextInt", "(I)I");

	javaObject = env->NewObject(javaClass, randomSeededConstructorMethod, seed);


}

int JavaRandom::nextInt(int bound) {
	return env->CallIntMethod(javaObject, nextIntMethod, bound);
}