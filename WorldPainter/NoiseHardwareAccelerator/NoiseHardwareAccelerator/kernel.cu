
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#include "org_pepsoft_worldpainter_exporting_gpuacceleration_ResourceNoiseGenerationRequest.h"
#include <stdlib.h>
#include "NoiseGeneration.h"
#include <windows.h>
#include <jni.h>
#include <thrust/device_vector.h>
#include <thrust/copy.h>
#include "JavaClassWrappers/GPUNoiseRequest.h"
#include "Inputs/NoiseInput.h"
#include "JavaClassWrappers/JavaRandom.h"

#include <chrono>
#include <stdio.h>
#include <ctime>

#define TILE_SIZE 128
#define REGION_SIZE 512
#define CHUNK_SIZE 16
#define MAX_HEIGHT 128
#define MIN_HEIGHT -64
#define X_ARRAY_SIZE 512
#define Y_ARRAY_SIZE 512
#define DEBUGGING false
#define height 32

cudaError_t noiseWithCuda(float* chances, NoiseInput& noiseInput, int*& output, int& outputSize, int totalHeight, GPUMemoryBlock gpuMemoryBlock);
void getRegionArray(float* regionArrayX, float* regionArrayY, float* regionArrayZ, ResourceNoiseGenerationRequest resourceNoiseGenerationRequest);
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
cudaError_t noiseWithCuda(float* chances, NoiseInput* noiseInput, int*& output, int& outputSize, int totalHeight, GPUMemoryBlock gpuMemoryBlock)
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

    if (gpuMemoryBlock.getpGPUPointer() == 0){
        cudaStatus = cudaMalloc((void**)&dev_p, 512 * sizeof(int));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_p = (int*)gpuMemoryBlock.getpGPUPointer();
    }

    cudaStatus = cudaMalloc((void**)&dev_chances, 16 * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    if (gpuMemoryBlock.getxGPUPointer() == 0) {
    cudaStatus = cudaMalloc((void**)&dev_regionArrayX, X_ARRAY_SIZE * sizeof(float));
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMalloc failed!");
        fprintf(stderr, cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }
    }
    else { //reuse
        dev_regionArrayX = (float*)gpuMemoryBlock.getxGPUPointer();
    }

    if (gpuMemoryBlock.getyGPUPointer() == 0) {
        cudaStatus = cudaMalloc((void**)&dev_regionArrayY, Y_ARRAY_SIZE * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_regionArrayY = (float*)gpuMemoryBlock.getyGPUPointer();
    }

    if (gpuMemoryBlock.getzGPUPointer() == 0) {
        cudaStatus = cudaMalloc((void**)&dev_regionArrayZ, height * sizeof(float));
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_regionArrayZ = (float*)gpuMemoryBlock.getzGPUPointer();
    }

    if (gpuMemoryBlock.getOutputGPUPointer() == 0)
    {
        cudaStatus = cudaMalloc((void**)&dev_output,sizeof(bool)* MAX_OUTPUT_SIZE); //Varies by height but roughly 25MB
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else { //reuse
        dev_output = (bool*)gpuMemoryBlock.getOutputGPUPointer();
    }
    if (gpuMemoryBlock.getCompactedOutputGPUPointer() == 0) {
        cudaStatus = cudaMalloc((void**)&dev_CompactedOutput, (MAX_OUTPUT_SIZE) * sizeof(int)); // Varies by height but roughly 100MB
        if (cudaStatus != cudaSuccess) {
            fprintf(stderr, "cudaMalloc failed!");
            freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
            return cudaStatus;
        }
    }
    else {
        dev_CompactedOutput = (int*)gpuMemoryBlock.getCompactedOutputGPUPointer();
    }

    // Copy input vectors from host memory to GPU buffers.
    cudaStatus = cudaMemcpy(dev_p, noiseInput->getPArray(), 512 * sizeof(int), cudaMemcpyHostToDevice);
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

    cudaStatus = cudaMemcpy(dev_regionArrayX, noiseInput->getRegionArrayX(), X_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayY, noiseInput->getRegionArrayY(), Y_ARRAY_SIZE * sizeof(float), cudaMemcpyHostToDevice);
    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "cudaMemcpy failed!", cudaGetErrorString(cudaStatus));
        freeCudaMemory(dev_p, dev_regionArrayX, dev_regionArrayY, dev_regionArrayZ, dev_output, dev_CompactedOutput);
        return cudaStatus;
    }

    cudaStatus = cudaMemcpy(dev_regionArrayZ, noiseInput->getRegionArrayZ(), height * sizeof(float), cudaMemcpyHostToDevice);
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
    gpuMemoryBlock.setpGPUPointer((long long)dev_p);
    gpuMemoryBlock.setxGPUPointer((long long)dev_regionArrayX);
    gpuMemoryBlock.setyGPUPointer((long long)dev_regionArrayY);
    gpuMemoryBlock.setzGPUPointer((long long)dev_regionArrayZ);
    gpuMemoryBlock.setOutputGPUPointer((long long)dev_output);
    gpuMemoryBlock.setCompactedOutputGPUPointer((long long)dev_CompactedOutput);

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

jobject createResponse(JNIEnv* env, int size, int* output, GPUMemoryBlock gpuMemoryBlock) {
    jclass noiseHardwareAcceleratorResponseClass = env->FindClass("org/pepsoft/worldpainter/exporting/NoiseHardwareAcceleratorResponse");
    jclass gpuMemoryBlockClass = env->FindClass("org/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock");

    jmethodID responseConstructorMethod = env->GetMethodID(noiseHardwareAcceleratorResponseClass, "<init>", "([ILorg/pepsoft/worldpainter/exporting/gpuacceleration/GPUMemoryBlock;)V");

    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, (jint*)output);

    delete[] output;

    jobject gpuMemoryBlockObject = gpuMemoryBlock.getJavaObject();
    jobject response = env->NewObject(noiseHardwareAcceleratorResponseClass, responseConstructorMethod, result, gpuMemoryBlockObject);

    return response;
}

JNIEXPORT jobject JNICALL Java_org_pepsoft_worldpainter_exporting_gpuacceleration_ResourceNoiseGenerationRequest_getResourceRegionNoiseData(JNIEnv* env, jobject obj, jobject gpuNoiseRequestObject) {
    std::clock_t c_start = std::clock();

    GPUNoiseRequest gpuNoiseRequest = GPUNoiseRequest(env, gpuNoiseRequestObject);
    ResourceNoiseGenerationRequest resourceNoiseGenerationRequest = gpuNoiseRequest.getResourcesNoiseGenerationRequest();
    GPUMemoryBlock gpuMemoryBlock = gpuNoiseRequest.getGPUMemoryBlock();

    int outputSize;
    int* outputArray;


    const int totalHeight = resourceNoiseGenerationRequest.getMaxHeight() - resourceNoiseGenerationRequest.getMinHeight();

    JavaRandom random = JavaRandom(env, resourceNoiseGenerationRequest.getSeed());

    NoiseInput*  resourceNoiseInput =new NoiseInput(resourceNoiseGenerationRequest, random);

    cudaError_t cudaStatus = noiseWithCuda(resourceNoiseGenerationRequest.getChances(),resourceNoiseInput, outputArray, outputSize, totalHeight, gpuMemoryBlock);

    if (cudaStatus != cudaSuccess) {
        fprintf(stderr, "addWithCuda failed!");
        return NULL;
    }

    jobject result = createResponse(env,outputSize, outputArray, gpuMemoryBlock);

    std::clock_t c_end = std::clock();
    double time_elapsed_ms = 1000.0 * (c_end - c_start) / CLOCKS_PER_SEC;
    if (DEBUGGING) printf("Finished in: %f Clocktime: %lf\n\n", time_elapsed_ms);
    
    return result;
}
