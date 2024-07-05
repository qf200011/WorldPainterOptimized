
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include "org_pepsoft_worldpainter_exporting_gpuacceleration_ResourceNoiseGenerationRequest.h"
#include <stdlib.h>
#include "NoiseGeneration.h"
#include <windows.h>
#include <jni.h>
#include <thrust/device_vector.h>
#include <thrust/copy.h>

#include <chrono>
#include <stdio.h>
#include <ctime>

#define TILE_SIZE 128
#define REGION_SIZE 512
#define CHUNK_SIZE 16
#define TILES_PER_REGION_AXIS 4
#define CHUNKS_PER_TILE_AXIS 8
#define MAX_HEIGHT 128
#define MIN_HEIGHT -64
#define X_ARRAY_SIZE 512
#define Y_ARRAY_SIZE 512
#define DEBUGGING false
#define height 32

cudaError_t noiseWithCuda(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, bool* output, int& outputSize, int totalHeight, long long& dev_regionArrayXPtr, long long& dev_regionArrayYPtr, long long& dev_regionArrayZPtr, long long& dev_pPtr, long long& dev_outputPtr, long long& dev_compactedOutputPtr);
void getRegionArray(float* regionArrayX, float* regionArrayY, float* regionArrayZ, int minHeight, int maxHeight, int heightOffset, int regionX, int regionY, float blobSize);
void getPArray(int* p, JNIEnv* env, jlong seed);
void swap(int* array, int index1, int index2);
void freeCudaMemory(int* dev_p, float* dev_regionArrayX, float* dev_regionArrayY, float* dev_regionArrayZ, bool* dev_output, int* dev_compactedOutput);


__global__ void generateNoise(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, bool *output, int totalHeight)
{
    float outputNoise = getPerlinNoiseAt(regionArrayX[blockIdx.x], regionArrayY[blockIdx.y], regionArrayZ[threadIdx.x], p);

    bool shouldSetMaterial = outputNoise >= chances[8];

    output[blockIdx.x + (blockIdx.y * X_ARRAY_SIZE) + (threadIdx.x * X_ARRAY_SIZE * X_ARRAY_SIZE)] = shouldSetMaterial;

}

struct is_true {
    __host__ __device__
        bool operator() (const bool success) {
        return success;
    }
};



// Helper function for using CUDA to add vectors in parallel.
cudaError_t noiseWithCuda(int* p, float* chances, float* regionArrayX, float* regionArrayY, float* regionArrayZ, int*&  output,int& outputSize, int totalHeight, long long &dev_regionArrayXPtr, long long &dev_regionArrayYPtr, long long& dev_regionArrayZPtr, long long& dev_pPtr, long long &dev_outputPtr, long long& dev_compactedOutputPtr)
{
    std::clock_t c_start = std::clock();

    int* dev_p;
    float* dev_regionArrayX;
    float* dev_regionArrayY;
    float* dev_regionArrayZ;
    bool* dev_output;
    int* dev_CompactedOutput;
    float* dev_chances;

    const int MAX_OUTPUT_SIZE = X_ARRAY_SIZE * Y_ARRAY_SIZE * height;
    const int CURRENT_OUTPUT_SIZE = X_ARRAY_SIZE * Y_ARRAY_SIZE * totalHeight;


    cudaError_t cudaStatus;
    auto t_start = std::chrono::high_resolution_clock::now();
    // Choose which GPU to run on, change this on a multi-GPU system.
    cudaStatus = cudaSetDevice(0);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaSetDevice failed!  Do you have a CUDA-capable GPU installed?");
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    if (dev_pPtr == 0){
        cudaStatus = cudaMalloc((void**)&dev_p, 512 * sizeof(int));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_p = (int*)dev_pPtr;
    }

    cudaStatus = cudaMalloc((void**)&dev_chances, 16 * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    if (dev_regionArrayXPtr==0){
    cudaStatus = cudaMalloc((void**)&dev_regionArrayX, X_ARRAY_SIZE * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        fprintf(stderr, cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }
    }
    else { //reuse
        dev_regionArrayX = (float*)dev_regionArrayXPtr;
    }

    if (dev_regionArrayYPtr == 0) {
        cudaStatus = cudaMalloc((void**)&dev_regionArrayY, Y_ARRAY_SIZE * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_regionArrayY = (float*)dev_regionArrayYPtr;
    }

    if (dev_regionArrayZPtr == 0){
        cudaStatus = cudaMalloc((void**)&dev_regionArrayZ, height * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_regionArrayZ = (float*)dev_regionArrayZPtr;
    }

    if (dev_outputPtr==0)
    {
        cudaStatus = cudaMalloc((void**)&dev_output,sizeof(bool)* MAX_OUTPUT_SIZE); //Varies by height but roughly 25MB
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_output = (bool*)dev_outputPtr;
    }
    if (dev_compactedOutputPtr == 0) {
        cudaStatus = cudaMalloc((void**)&dev_CompactedOutput, (MAX_OUTPUT_SIZE) * sizeof(int)); // Varies by height but roughly 100MB
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else {
        dev_CompactedOutput = (int*)dev_compactedOutputPtr;
    }

    // Copy input vectors from host memory to GPU buffers.
    cudaStatus = cudaMemcpy(dev_p, p, 512 * sizeof(int), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    // Copy input vectors from host memory to GPU buffers.
    cudaStatus = cudaMemcpy(dev_chances, chances, 16 * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayX, regionArrayX, X_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayY, regionArrayY, Y_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayZ, regionArrayZ, height * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    std::clock_t c_end = std::clock();
    double time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Clocktime for inputs: %lf\n", time_elapsed_ms);







    // Launch a kernel on the GPU with one thread for each element.
    cudaStream_t stream;
    cudaStreamCreate(&stream);
    dim3 gridShape(X_ARRAY_SIZE, Y_ARRAY_SIZE, 1);
    generateNoise <<<gridShape, totalHeight,0, stream >>>(dev_p,dev_chances, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output,totalHeight);

    // Check for any errors launching the kernel
    cudaStatus = cudaGetLastError();
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "addKernel launch failed: %s\n", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Clocktime for computation before sleep: %lf\n", time_elapsed_ms);
    
    // cudaDeviceSynchronize waits for the kernel to finish, and returns
    // any errors encountered during the launch.
    while (cudaStreamQuery(stream) == cudaErrorNotReady) {
        Sleep(10);
    }

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING)printf("Clocktime for computation before synchronize: %lf\n", time_elapsed_ms);

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Clocktime for computation after synchronize: %lf\n", time_elapsed_ms);

    thrust::device_ptr<bool> t_output(dev_output);
    thrust::device_ptr<int> t_compactedOutput(dev_CompactedOutput);
    thrust::device_vector<bool> d_outputVector(t_output, t_output + CURRENT_OUTPUT_SIZE);
    thrust::device_vector<int> d_compactedOutputVector(t_compactedOutput, t_compactedOutput + CURRENT_OUTPUT_SIZE);

    thrust::device_vector<int>::iterator t_compactedOutputEnd =
        thrust::copy_if(thrust::make_counting_iterator<int>(0), thrust::make_counting_iterator<int>(CURRENT_OUTPUT_SIZE), d_outputVector.begin(), d_compactedOutputVector.begin(), is_true());

    outputSize = thrust::distance(d_compactedOutputVector.begin(), t_compactedOutputEnd);

    output = new int[outputSize];

    thrust::copy(d_compactedOutputVector.begin(), t_compactedOutputEnd, output);

    //save pointers for reuse
    dev_pPtr = (long long)dev_p;
    dev_regionArrayXPtr = (long long)dev_regionArrayX;
    dev_regionArrayYPtr = (long long)dev_regionArrayY;
    dev_regionArrayZPtr = (long long)dev_regionArrayZ;
    dev_outputPtr = (long long)dev_output;
    dev_compactedOutputPtr = (long long)dev_CompactedOutput;

    cudaFree(dev_chances);

    c_end = std::clock();
    time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Clocktime for after copying results: %lf\n", time_elapsed_ms);
    
    return cudaStatus;
}



void freeCudaMemory(int* dev_p, float* dev_regionArrayX, float* dev_regionArrayY, float* dev_regionArrayZ, bool* dev_output, int* dev_compactedOutput) {
    cudaFree(dev_regionArrayX);
    cudaFree(dev_regionArrayY);
    cudaFree(dev_regionArrayZ);
    cudaFree(dev_output);
    cudaFree(dev_compactedOutput);
}


void getRegionArray(float* regionArrayX, float* regionArrayY, float* regionArrayZ, int minHeight, int maxHeight, int regionX, int regionY, float blobSize, int heightOffset) {

    int minTileX = regionX * 4;
    int minTileY = regionY * 4;

    for (int tileX = 0; tileX < TILES_PER_REGION_AXIS; tileX++) {
        for (int x = 0; x < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; x++) {
            int worldX = (tileX+minTileX)*TILE_SIZE + x;
            regionArrayX[tileX * TILE_SIZE + x] = worldX / blobSize;
        }
    }

    for (int tileY = 0; tileY < TILES_PER_REGION_AXIS; tileY++) {
        for (int y = 0; y < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; y++) {
            int worldY = (tileY+minTileY)*TILE_SIZE + y;
            regionArrayY[tileY * TILE_SIZE + y] = worldY / blobSize;
        }
    }
    int totalHeight = maxHeight - minHeight;

    for (int z = 0; z < totalHeight; z++) {
        regionArrayZ[z] = (z+minHeight) / blobSize;
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

void getDataFromRequest(JNIEnv* env, jobject request, jlong& materialSeed, jint& regionX, jint& regionY, jint& materialMinHeight, jint& materialMaxHeight, jint& heightOffset, jfloat& blobSize, jlong& regionXPtr, jlong& regionYPtr, jlong& regionZPtr, jlong& pPtr, jlong& outputPtr, jlong& compactedOutputPtr, int*& outputArray, float*& chances) {
    jclass gpuNoiseRequestClass = env->FindClass("org/pepsoft/worldpainter/exporting/NoiseHardwareAccelerator$GPUNoiseRequest");
    jclass gpuMemoryBlockClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock");
    
    jclass noiseHardwareAcceleratorRequestClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/ResourceNoiseGenerationRequest");

    jmethodID getNoiseRequestMethod = env->GetMethodID(gpuNoiseRequestClass, "getNoiseGenerationRequest", "()Lorg/pepsoft/worldpainter/exporting/gpuacceleration/NoiseGenerationRequest;");
    jobject noiseGnerationRequest = env->CallObjectMethod(request, getNoiseRequestMethod);

    jmethodID getMaterialSeedMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getSeed", "()J");
    jmethodID getRegionXMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionX", "()I");
    jmethodID getRegionYMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getRegionY", "()I");
    jmethodID getMaterialMinHeightMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getMinHeight", "()I");
    jmethodID getMaterialMaxHeightMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getMaxHeight", "()I");
    jmethodID getMaterialHeightOffset = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getHeightOffset", "()I");
    jmethodID getBlobSizeMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getBlobSize", "()F");
    jmethodID getChancesMethod = env->GetMethodID(noiseHardwareAcceleratorRequestClass, "getChances", "()[F");

    jmethodID getGPUMemoryBlockMethodId = env->GetMethodID(gpuNoiseRequestClass, "getGpuMemoryBlock", "()Lorg/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock;");
    jobject gpuMemoryBlock = env->CallObjectMethod(request, getGPUMemoryBlockMethodId);

    jmethodID getxGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getxGPUPointer", "()J");
    jmethodID getyGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getyGPUPointer", "()J");
    jmethodID getzGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getzGPUPointer", "()J");
    jmethodID getpGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getpGPUPointer", "()J");
    jmethodID getOutputGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getOutputGPUPointer", "()J");
    jmethodID getCompactedGPUPointerMethod = env->GetMethodID(gpuMemoryBlockClass, "getCompactedGPUPointer", "()J");

    materialSeed = env->CallLongMethod(noiseGnerationRequest, getMaterialSeedMethod);
    regionX = env->CallIntMethod(noiseGnerationRequest, getRegionXMethod);
    regionY = env->CallIntMethod(noiseGnerationRequest, getRegionYMethod);
    materialMinHeight = env->CallIntMethod(noiseGnerationRequest, getMaterialMinHeightMethod);
    materialMaxHeight = env->CallIntMethod(noiseGnerationRequest, getMaterialMaxHeightMethod);
    heightOffset = env->CallIntMethod(noiseGnerationRequest, getMaterialHeightOffset);
    blobSize = env->CallFloatMethod(noiseGnerationRequest, getBlobSizeMethod);
    regionXPtr = env->CallLongMethod(gpuMemoryBlock, getxGPUPointerMethod);
    regionYPtr = env->CallLongMethod(gpuMemoryBlock, getyGPUPointerMethod);
    regionZPtr = env->CallLongMethod(gpuMemoryBlock, getzGPUPointerMethod);
    pPtr = env->CallLongMethod(gpuMemoryBlock, getpGPUPointerMethod);
    outputPtr = env->CallLongMethod(gpuMemoryBlock, getOutputGPUPointerMethod);
    compactedOutputPtr = env->CallLongMethod(gpuMemoryBlock, getCompactedGPUPointerMethod);
    jfloatArray chancesArray = (jfloatArray)env->CallObjectMethod(noiseGnerationRequest, getChancesMethod);
    chances = env->GetFloatArrayElements(chancesArray, 0);
}


jobject createResponse(JNIEnv* env, int size, int* output, long long dev_regionXPtr, long long dev_regionYPtr, long long dev_regionZPtr, long long dev_pPtr, long long dev_outputPtr, long long dev_compactedOutputPtr, int totalHeight) {
    jclass noiseHardwareAcceleratorResponseClass = env->FindClass("org/pepsoft/worldpainter/exporting/NoiseHardwareAcceleratorResponse");
    jclass gpuMemoryBlockClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock");

    jmethodID responseConstructorMethod = env->GetMethodID(noiseHardwareAcceleratorResponseClass, "<init>", "([ILorg/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock;)V");
    jmethodID gpuMemoryConstructorMethod = env->GetMethodID(gpuMemoryBlockClass, "<init>", "(JJJJJJ)V");

    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, (jint*)output);

    delete[] output;

    jlong pPtr = (jlong)dev_pPtr;
    jlong regionXPtr = (jlong)dev_regionXPtr;
    jlong regionYPtr = (jlong)dev_regionYPtr;
    jlong regionZPtr = (jlong)dev_regionZPtr;
    jlong outputPtr = (jlong)dev_outputPtr;
    jlong compactedOutputPtr = (jlong)dev_compactedOutputPtr;

    jobject gpuMemoryBlock = env->NewObject(gpuMemoryBlockClass, gpuMemoryConstructorMethod, regionXPtr, regionYPtr, regionZPtr, pPtr, outputPtr,compactedOutputPtr);
    jobject response = env->NewObject(noiseHardwareAcceleratorResponseClass, responseConstructorMethod, result, gpuMemoryBlock);

    return response;
}

JNIEXPORT jobject JNICALL Java_org_pepsoft_worldpainter_exporting_gpuacceleration_ResourceNoiseGenerationRequest_getResourceRegionNoiseData(JNIEnv* env, jobject obj, jobject request) {
    std::clock_t c_start = std::clock();

    jlong materialSeed;
    jint regionX;
    jint regionY;
    jint materialMinHeight;
    jint materialMaxHeight;
    jint heightOffset;
    jlong dev_regionXPtr;
    jlong dev_regionYPtr;
    jlong dev_regionZPtr;
    jlong dev_pPtr;
    jlong dev_outputPtr;
    jlong dev_compactedOutputPtr;
    int* outputArray;
    float* chances;
    int outputSize;
    float blobSize;

    getDataFromRequest(env, request, materialSeed, regionX, regionY, materialMinHeight, materialMaxHeight,heightOffset, blobSize, dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr, dev_outputPtr, dev_compactedOutputPtr, outputArray, chances);


    const int totalHeight = materialMaxHeight - materialMinHeight;

    float regionArrayX[X_ARRAY_SIZE]; //dx
    float regionArrayY[Y_ARRAY_SIZE]; //dy
    float* regionArrayZ; //dz but shifted
    int p[512];
    //[TILE_SIZE * TILES_PER_REGION_AXIS] [TILE_SIZE * TILES_PER_REGION_AXIS] [totalHeight]
    regionArrayZ = new float[totalHeight];

    getRegionArray(regionArrayX, regionArrayY, regionArrayZ, materialMinHeight, materialMaxHeight, regionX, regionY,blobSize,heightOffset);


    getPArray(p, env, materialSeed);
    //double test = getPerlinNoiseAt(regionArrayX[128], regionArrayY[128], regionArrayZ[60 + 64],p);


    // Add vectors in parallel.

    cudaError_t cudaStatus = noiseWithCuda(p, chances, regionArrayX, regionArrayY, regionArrayZ, outputArray, outputSize, totalHeight, dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr, dev_outputPtr, dev_compactedOutputPtr);

    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "addWithCuda failed!");
        delete[] regionArrayZ;
        return NULL;
    }

    jobject result = createResponse(env,outputSize, outputArray, dev_regionXPtr, dev_regionYPtr, dev_regionZPtr, dev_pPtr,dev_outputPtr, dev_compactedOutputPtr, totalHeight);
   
    delete[] regionArrayZ;

    std::clock_t c_end = std::clock();
    double time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Finished in: %f Clocktime: %lf\n\n", time_elapsed_ms);
    
    return result;
}
