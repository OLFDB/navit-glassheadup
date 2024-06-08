#include "zonedetect.h"
#include <jni.h>
#include <android/log.h>
#include <string.h>
#define TAG "ConversionHelper"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,    TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,     TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,     TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,    TAG, __VA_ARGS__)

void onError(int errZD, int errNative)
{
    __android_log_write(ANDROID_LOG_ERROR, "Tag", ZDGetErrorString(errZD));
}

JNIEXPORT jstring JNICALL
Java_org_navitproject_glassheadup_ConversionHelper_getTimezone(
                                                                       JNIEnv* env,
                                                                       jclass classInstance,
                                                                       jdouble lat,
                                                                       jdouble lon
                                                                       
                                                                       ) {
    ZDSetErrorHandler(onError);
    ZoneDetect *const cd = ZDOpenDatabase("/data/data/org.navitproject.glassheadup/files/timezone21.bin");
    //                                          /data/media/0/Android/data/org.navitproject.glassheadup/files/timezone21.bin

    if(cd == NULL) {
        LOGD("--------------------------------------------------------------------------------------> ZONEDETECT lib load NULL!!!");
        return (*env)->NewStringUTF(env, NULL);
    }

    float safezone = 0;
    ZoneDetectResult *results = ZDLookup(cd, lat, lon, &safezone);
    char timezone[100];
    timezone[0]=0;
    unsigned int index = 0;
    
    while(results[index].lookupResult != ZD_LOOKUP_END) {
        LOGD("%s:\n", ZDLookupResultToString(results[index].lookupResult));
        LOGD("  meta: %u\n", results[index].metaId);
        LOGD("  polygon: %u\n", results[index].polygonId);
        if(results[index].data) {
            for(unsigned int i = 0; i < results[index].numFields; i++) {
                if(results[index].fieldNames[i] && results[index].data[i]) {
                    LOGD("%s: %s\n", results[index].fieldNames[i], results[index].data[i]);
                    strcat(timezone, results[index].data[i]);
                    if(i==1)
                        break;
                }
            }
            LOGD("Timezone: %s\n", timezone);
        }
        
        index++;
    }

    ZDFreeResults(results);
    ZDCloseDatabase(cd);
    
    return (*env)->NewStringUTF(env, timezone);;
}

