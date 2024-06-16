
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include "org_pepsoft_worldpainter_exporting_NoiseHardwareAccelerator.h"
#include <jni.h>
#include <stdlib.h>
#include "NoiseGeneration.h"
#include <windows.h>

#include <chrono>
#include <stdio.h>
#include <ctime>


#define TINY_BLOBS 4.099f
#define TILE_SIZE 128
#define REGION_SIZE 512
#define CHUNK_SIZE 16
#define TILES_PER_REGION_AXIS 5
#define CHUNKS_PER_TILE_AXIS 8
#define MAX_HEIGHT 128
#define MIN_HEIGHT -64
#define X_ARRAY_SIZE 680
#define Y_ARRAY_SIZE 680

cudaError_t noiseWithCuda(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, byte* output, int totalHeight, long long& dev_regionArrayXPtr, long long& dev_regionArrayYPtr, long long& dev_regionArrayZPtr, long long& dev_pPtr, long long& dev_outputPtr);
void getRegionArray(float* regionArrayX, float* regionArrayY, float* regionArrayZ, int minHeight, int maxHeight, int regionX, int regionY);
void getPArray(int* p, JNIEnv* env, jlong seed);
void swap(int* array, int index1, int index2);


__global__ void generateNoise(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, byte *output, int totalHeight)
{
    /*int x=128;
    int y=128;
    int z=64+61;
    printf("P is %d\n", p[131]);
    output[x+ y*Y_ARRAY_SIZE + z* Z_ARRAY_SIZE]= getPerlinNoiseAt(regionArrayX[x], regionArrayY[y], regionArrayZ[z], p);*/

    /*if (blockIdx.x == 128 && blockIdx.y == 128 && threadIdx.x == 125) {
        printf("Special value: %f in spot %d\n", getPerlinNoiseAt(regionArrayX[blockIdx.x], regionArrayY[blockIdx.y], regionArrayZ[threadIdx.x], p), blockIdx.x + (blockIdx.y * X_ARRAY_SIZE) + (threadIdx.x * X_ARRAY_SIZE * Y_ARRAY_SIZE));
    }

    if (blockIdx.x + (blockIdx.y * X_ARRAY_SIZE) + (threadIdx.x * X_ARRAY_SIZE * Y_ARRAY_SIZE) == 122432) {
        printf("I hit the index with X:%d Y:%d Z:%d\n", blockIdx.x, blockIdx.y, threadIdx.x);
        printf("(%d + %d  * %d ) + (%d  * %d )", blockIdx.x, blockIdx.y, Y_ARRAY_SIZE, threadIdx.x, Z_ARRAY_SIZE);
    }*/

    float outputNoise = getPerlinNoiseAt(regionArrayX[blockIdx.x], regionArrayY[blockIdx.y], regionArrayZ[threadIdx.x], p);


    bool shouldSetMaterial = outputNoise >= chances[8];



    int index = blockIdx.x + (blockIdx.y * X_ARRAY_SIZE) + (threadIdx.x * X_ARRAY_SIZE * totalHeight);
    int byteIndex = index / 8;
    int bitIndex = index % 8;

    if (shouldSetMaterial && bitIndex == 0) {
        output[byteIndex] |= (1 << bitIndex);
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 1) {
        output[byteIndex] |= (1 << bitIndex);
       
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 2) {
        output[byteIndex] |= (1 << bitIndex);
        
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 3) {
        output[byteIndex] |= (1 << bitIndex);
        
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 4) {
        output[byteIndex] |= (1 << bitIndex);
        
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 5) {
        output[byteIndex] |= (1 << bitIndex);
        
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 6) {
        output[byteIndex] |= (1 << bitIndex);
        
    }
    __syncthreads();
    if (shouldSetMaterial && bitIndex == 7) {
        output[byteIndex] |= (1 << bitIndex);
        output[blockIdx.x + (blockIdx.y * X_ARRAY_SIZE) + (threadIdx.x * X_ARRAY_SIZE * totalHeight)];
    }

}

int main()
{
    //float regionArrayX[X_ARRAY_SIZE]; //dx
    //float regionArrayY[Y_ARRAY_SIZE]; //dy
    ////float regionArrayZ[Z_ARRAY_SIZE]; //dz but shifted
    //int p[512];
    ////float output[X_ARRAY_SIZE * Y_ARRAY_SIZE * Z_ARRAY_SIZE];
    ////[TILE_SIZE * TILES_PER_REGION_AXIS] [TILE_SIZE * TILES_PER_REGION_AXIS] [totalHeight]
    ////getRegionArray(regionArrayX, regionArrayY, regionArrayZ, Z_ARRAY_SIZE, 0, 0);
    ////getPArray(p);
 
    ///*for (int i = 0; i < 512; i++) {
    //    printf("%d: %d\n", i, p[i]);
    //}*/

    //// Add vectors in parallel.

    ////cudaError_t cudaStatus = noiseWithCuda(p, regionArrayX, regionArrayY, regionArrayZ, output);
    //if (cudaStatus != cudaSuccess) {
    //    fprintf(stderr, "addWithCuda failed!");
    //    delete[] regionArrayX;
    //    delete[] regionArrayY;
    //    delete[] regionArrayZ;
    //    delete[] output;
    //    delete[] p;
    //    return 1;
    //}

    //delete[] regionArrayX;
    //delete[] regionArrayY;
    //delete[] regionArrayZ;
    //delete[] output;
    //delete[] p;
    ///*for (int i= 0; i < X_ARRAY_SIZE * Y_ARRAY_SIZE * Z_ARRAY_SIZE; i++) {
    //    printf("%d: %.6f\n", i, output[i]);
    //}*/

    //// cudaDeviceReset must be called before exiting in order for profiling and
    //// tracing tools such as Nsight and Visual Profiler to show complete traces.
    //cudaStatus = cudaDeviceReset();
    //if (cudaStatus != cudaSuccess) {
    //    fprintf(stderr, "cudaDeviceReset failed!");
    //    return 1;
    //}

    //return 0;
}

// Helper function for using CUDA to add vectors in parallel.
cudaError_t noiseWithCuda(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, byte*  output,int totalHeight, long long &dev_regionArrayXPtr, long long &dev_regionArrayYPtr, long long& dev_regionArrayZPtr, long long& dev_pPtr, long long &dev_outputPtr)
{
    std::clock_t c_start = std::clock();

    int* dev_p;
    float* dev_regionArrayX;
    float* dev_regionArrayY;
    float* dev_regionArrayZ;
    byte* dev_output;
    float* dev_chances;

    cudaError_t cudaStatus;
    auto t_start = std::chrono::high_resolution_clock::now();
    // Choose which GPU to run on, change this on a multi-GPU system.
    cudaStatus = cudaSetDevice(0);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaSetDevice failed!  Do you have a CUDA-capable GPU installed?");
        goto Error;
    }

    if (dev_pPtr == 0){
        cudaStatus = cudaMalloc((void**)&dev_p, 512 * sizeof(int));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            goto Error;
        }
    }
    else { //reuse
        dev_p = (int*)dev_pPtr;
    }

    cudaStatus = cudaMalloc((void**)&dev_chances, 16 * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        goto Error;
    }

    if (dev_regionArrayXPtr==0){
    cudaStatus = cudaMalloc((void**)&dev_regionArrayX, X_ARRAY_SIZE * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        fprintf(stderr, cudaGetErrorString(cudaStatus));
        goto Error;
    }
    }
    else { //reuse
        dev_regionArrayX = (float*)dev_regionArrayXPtr;
    }

    if (dev_regionArrayYPtr == 0) {
        cudaStatus = cudaMalloc((void**)&dev_regionArrayY, Y_ARRAY_SIZE * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            goto Error;
        }
    }
    else { //reuse
        dev_regionArrayY = (float*)dev_regionArrayYPtr;
    }

    if (dev_regionArrayZPtr == 0){
        cudaStatus = cudaMalloc((void**)&dev_regionArrayZ, totalHeight * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            goto Error;
        }
    }
    else { //reuse
        dev_regionArrayZ = (float*)dev_regionArrayZPtr;
    }

    if (dev_outputPtr==0)
    {
        cudaStatus = cudaMalloc((void**)&dev_output, (X_ARRAY_SIZE * Y_ARRAY_SIZE * totalHeight)); //going to be bits
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            goto Error;
        }
    }
    else { //reuse
        dev_output = (byte*)dev_outputPtr;
    }

    // Copy input vectors from host memory to GPU buffers.
    cudaStatus = cudaMemcpy(dev_p, p, 512 * sizeof(int), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    // Copy input vectors from host memory to GPU buffers.
    cudaStatus = cudaMemcpy(dev_chances, chances, 16 * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayX, regionArrayX, X_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayY, regionArrayY, Y_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayZ, regionArrayZ, totalHeight * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }
    auto t_end = std::chrono::high_resolution_clock::now();
    double elapsed_time_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();
    //printf("Allocated Memory: %f\n", elapsed_time_ms);

    std::clock_t c_end = std::clock();
    double time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    printf("Clocktime for inputs: %lf\n", time_elapsed_ms);

    // Launch a kernel on the GPU with one thread for each element.
    cudaStream_t stream;
    cudaStreamCreate(&stream);
    dim3 gridShape(X_ARRAY_SIZE, Y_ARRAY_SIZE, 1);
    generateNoise <<<gridShape, totalHeight,0, stream >>>(dev_p,dev_chances, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output,totalHeight);

    // Check for any errors launching the kernel
    cudaStatus = cudaGetLastError();
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "addKernel launch failed: %s\n", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    printf("Clocktime for computation before sleep: %lf\n", time_elapsed_ms);
    
    // cudaDeviceSynchronize waits for the kernel to finish, and returns
    // any errors encountered during the launch.
    while (cudaStreamQuery(stream) == cudaErrorNotReady) {
        printf("Time to sleep!");
        Sleep(10);
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    printf("Clocktime for computation before synchronize: %lf\n", time_elapsed_ms);


    cudaStatus = cudaDeviceSynchronize();
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaDeviceSynchronize returned error code %d after launching addKernel!\n", cudaStatus);
        goto Error;
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    printf("Clocktime for computation after synchronize: %lf\n", time_elapsed_ms);

    //save pointers for reuse
    dev_pPtr = (long long)dev_p;
    dev_regionArrayXPtr = (long long)dev_regionArrayX;
    dev_regionArrayYPtr = (long long)dev_regionArrayY;
    dev_regionArrayZPtr = (long long)dev_regionArrayZ;
    dev_outputPtr = (long long)dev_output;

    t_end = std::chrono::high_resolution_clock::now();
    elapsed_time_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();
    //printf("Finished computing: %f\n", elapsed_time_ms);

    // Copy output vector from GPU buffer to host memory.
    cudaStatus = cudaMemcpy(output, dev_output, (X_ARRAY_SIZE * Y_ARRAY_SIZE * totalHeight)/8, cudaMemcpyDeviceToHost);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        goto Error;
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    printf("Clocktime for after copying results: %lf\n", time_elapsed_ms);

Error:
    /*cudaFree(dev_p);
    cudaFree(dev_regionArrayX);
    cudaFree(dev_regionArrayY);
    cudaFree(dev_regionArrayZ);
    cudaFree(dev_output);*/

    cudaFree(dev_chances);
    
    return cudaStatus;
}


void getRegionArray(float* regionArrayX, float* regionArrayY, float* regionArrayZ, int minHeight, int maxHeight, int regionX, int regionY) {

    int minTileX = regionX * 4;
    int minTileY = regionY * 4;
    int maxTileX = minTileX + 5;
    int maxTileY = minTileY + 5;

    for (int tileX = 0; tileX < TILES_PER_REGION_AXIS; tileX++) {
        for (int x = 0; x < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; x++) {
            int worldX = (tileX+minTileX)*TILE_SIZE + x;
            regionArrayX[tileX * TILE_SIZE + x] = worldX / TINY_BLOBS;
        }
    }

    for (int tileY = 0; tileY < TILES_PER_REGION_AXIS; tileY++) {
        for (int y = 0; y < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; y++) {
            int worldY = (tileY+minTileY)*TILE_SIZE + y;
            regionArrayY[tileY * TILE_SIZE + y] = worldY / TINY_BLOBS;
        }
    }
    int totalHeight = maxHeight - minHeight;

    for (int z = 0; z < totalHeight; z++) {
        regionArrayZ[z] = (z-minHeight) / TINY_BLOBS;
    }
}

void getPArray(int* p, JNIEnv* env,jlong seed) {
    int permutation[256];

    for (int i=0; i < 256; i++) {
        permutation[i] = i;
    }

    jclass randomClass = env->FindClass("java/util/Random"); //use Java so we can keep the same seed.
    if (randomClass == NULL) {
        fprintf(stderr, "Unable to find java Random object");
        return;
    }
    jmethodID randomConstructor = env->GetMethodID(randomClass, "<init>", "(J)V");
    if (randomConstructor == NULL) {
        fprintf(stderr, "Unable to find java Random object constructor");
        return;
    }

    jmethodID nextIntMethod = env->GetMethodID(randomClass, "nextInt", "(I)I");

    jobject randomObject = env->NewObject(randomClass, randomConstructor, seed);


    for (int i = 256; i > 1; i--) {
        jint randomInt = env->CallIntMethod(randomObject, nextIntMethod, i); //random.NextInt(i)
        swap(permutation, i-1, randomInt);
    }

    for (int i = 0; i < 256; i++) {
        p[256 + i] = p[i] = permutation[i];
    }


}

void swap(int* array, int index1, int index2) {
    int temp = array[index1];
    array[index1] = array[index2];
    array[index2] = temp;
}

void getDataFromRequest(JNIEnv* env, jobject request, jlong& materialSeed, jint& regionX, jint& regionY, jint& materialMinHeight, jint& materialMaxHeight, jlong& regionXPtr, jlong& regionYPtr, jlong& regionZPtr, jlong& pPtr, jlong& outputPtr, byte*& outputArray, float*& chances) {
    jclass noiseHardwareAcceleratorRequestClass = env->FindClass("org/pepsoft/worldpainter/exporting/NoiseHardwareAcceleratorRequest");

    jmethodID getMaterialSeedMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getMaterialSeed", "()J");
    jmethodID getRegionXMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionX", "()I");
    jmethodID getRegionYMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionY", "()I");
    jmethodID getMaterialMinHeightMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getMaterialMinHeight", "()I");
    jmethodID getMaterialMaxHeightMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getMaterialMaxHeight", "()I");
    jmethodID getRegionXPtrMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionXPtr", "()J");
    jmethodID getRegionYPtrMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionYPtr", "()J");
    jmethodID getRegionZPtrMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionZPtr", "()J");
    jmethodID getpPtrMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getpPtr", "()J");
    jmethodID getOutputPtrMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getOutputPtr", "()J");
    jmethodID getOutputArrayMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getOutputArray", "()Ljava/nio/ByteBuffer;");
    jmethodID getChancesMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getChances", "()[F");

    materialSeed = env->CallLongMethod(request, getMaterialSeedMethod);
    regionX = env->CallIntMethod(request, getRegionXMethod);
    regionY = env->CallIntMethod(request, getRegionYMethod);
    materialMinHeight = env->CallIntMethod(request, getMaterialMinHeightMethod);
    materialMaxHeight = env->CallIntMethod(request, getMaterialMaxHeightMethod);
    regionXPtr = env->CallLongMethod(request, getRegionXPtrMethod);
    regionYPtr = env->CallLongMethod(request, getRegionYPtrMethod);
    regionZPtr = env->CallLongMethod(request, getRegionZPtrMethod);
    pPtr = env->CallLongMethod(request, getpPtrMethod);
    outputPtr = env->CallLongMethod(request, getOutputPtrMethod);
    jobject outputArrayBuffer =  env->CallObjectMethod(request, getOutputArrayMethod);
    outputArray = (byte*) env->GetDirectBufferAddress(outputArrayBuffer);
    jfloatArray chancesArray = (jfloatArray)env->CallObjectMethod(request, getChancesMethod);
    chances = env->GetFloatArrayElements(chancesArray, 0);
}


jobject createResponse(JNIEnv* env, long long dev_regionXPtr, long long dev_regionYPtr, long long dev_regionZPtr, long long dev_pPtr, long long dev_outputPtr, int totalHeight) {
    jclass noiseHardwareAcceleratorResponseClass = env->FindClass("org/pepsoft/worldpainter/exporting/NoiseHardwareAcceleratorResponse");

    jmethodID constructorMethod = env->GetMethodID(noiseHardwareAcceleratorResponseClass, "<init>", "([FJJJJJ)V");

    jlong pPtr= (jlong) dev_pPtr;
    jlong regionXPtr= (jlong) dev_regionXPtr;
    jlong regionYPtr = (jlong) dev_regionYPtr;
    jlong regionZPtr = (jlong) dev_regionZPtr;
    jlong outputPtr = (jlong) dev_outputPtr;

    jobject response = env->NewObject(noiseHardwareAcceleratorResponseClass, constructorMethod, NULL, regionXPtr, regionYPtr, regionZPtr, pPtr, outputPtr);

    return response;
}

JNIEXPORT jobject JNICALL Java_org_pepsoft_worldpainter_exporting_NoiseHardwareAccelerator_getRegionNoiseData (JNIEnv* env, jclass , jobject request) {
    auto t_start = std::chrono::high_resolution_clock::now();
    std::clock_t c_start = std::clock();

    jlong materialSeed;
    jint regionX;
    jint regionY;
    jint materialMinHeight;
    jint materialMaxHeight;
    jlong dev_regionXPtr;
    jlong dev_regionYPtr;
    jlong dev_regionZPtr;
    jlong dev_pPtr;
    jlong dev_outputPtr;
    byte* outputArray;
    float* chances;

    getDataFromRequest(env, request, materialSeed, regionX, regionY, materialMinHeight, materialMaxHeight, dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr, dev_outputPtr, outputArray, chances);


    const int totalHeight = materialMaxHeight - materialMinHeight;

    float regionArrayX[X_ARRAY_SIZE]; //dx
    float regionArrayY[Y_ARRAY_SIZE]; //dy
    float*  regionArrayZ; //dz but shifted
    int p[512];
    //[TILE_SIZE * TILES_PER_REGION_AXIS] [TILE_SIZE * TILES_PER_REGION_AXIS] [totalHeight]
    regionArrayZ = new float[totalHeight];
    getRegionArray(regionArrayX, regionArrayY, regionArrayZ, materialMaxHeight,materialMinHeight, regionX, regionY);

    
    getPArray(p,env,materialSeed);
    //double test = getPerlinNoiseAt(regionArrayX[128], regionArrayY[128], regionArrayZ[60 + 64],p);
    

    // Add vectors in parallel.

    cudaError_t cudaStatus = noiseWithCuda(p,chances, regionArrayX, regionArrayY, regionArrayZ, outputArray,totalHeight,dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr, dev_outputPtr);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "addWithCuda failed!");
        delete[] regionArrayZ;
        return NULL;
    }

    std::clock_t c_end = std::clock();
    double time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    //printf("Clocktime: %lf\n", time_elapsed_ms);

    // cudaDeviceReset must be called before exiting in order for profiling and
    // tracing tools such as Nsight and Visual Profiler to show complete traces.
    /*cudaStatus = cudaDeviceReset();
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaDeviceReset failed!");
        return NULL;
    }*/
    
    jobject result = createResponse(env,dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr,dev_outputPtr, totalHeight);
   
    delete[] regionArrayZ;

    auto t_end = std::chrono::high_resolution_clock::now();
    auto elapsed_time_ms = std::chrono::duration<double, std::milli>(t_end - t_start).count();
    //printf("Finished in: %f Clocktime: %lf\n\n", elapsed_time_ms,time_elapsed_ms);
    return result;
}
