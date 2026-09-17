#include <jni.h>
#include <string>
#include <cstdlib>
#include "node.h"

extern "C" JNIEXPORT jint JNICALL
Java_io_tyrizx_MainActivity_startNodeWithArguments(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arguments) {

    int argc = env->GetArrayLength(arguments);
    char** argv = new char*[argc];

    for (int i = 0; i < argc; i++) {
        jstring string = (jstring) env->GetObjectArrayElement(arguments, i);
        const char* rawString = env->GetStringUTFChars(string, 0);
        argv[i] = strdup(rawString);
        env->ReleaseStringUTFChars(string, rawString);
        env->DeleteLocalRef(string);
    }

    return node::Start(argc, argv);
}
