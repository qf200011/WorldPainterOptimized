#pragma once
#include "cuda_runtime.h"
#include "device_launch_parameters.h"


#include <cmath>
#include <stdio.h>

#define FACTOR_3D 0.4824607142760952

//__device__ double grad(int hash, double x, double y, double z);
//__device__ double lerp(double t, double a, double b);
//__device__ double fade(double t);
//
//__device__ double getNoiseAt(double x, double y, double z, int* p)
//{
//    int X = (int)floor(x) & 255;
//    int Y = (int)floor(y) & 255;
//    int Z = (int)floor(z) & 255;
//
//    x -= floor(x);
//    y -= floor(y);
//    z -= floor(z);
//
//    double u = fade(x);
//    double v = fade(y);
//    double w = fade(z);
//
    //int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z,      // HASH COORDINATES OF
    //    B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;      // THE 8 CUBE CORNERS,

    //return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),  // AND ADD
    //    grad(p[BA], x - 1, y, z)), // BLENDED
    //    lerp(u, grad(p[AB], x, y - 1, z),  // RESULTS
    //        grad(p[BB], x - 1, y - 1, z))),// FROM  8
    //    lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),  // CORNERS
    //        grad(p[BA + 1], x - 1, y, z - 1)), // OF CUBE
    //        lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
    //            grad(p[BB + 1], x - 1, y - 1, z - 1))));    
//}
//
//__device__ double getPerlinNoiseAt(double x, double y, double z, int* p) {
//    return (double) (getNoiseAt(x, y, z, p) * FACTOR_3D);
//}
//
//
//__device__ double fade(double t) {
//    return t * t * t * (t * (t * 6 - 15) + 10);
//}
//
//__device__ double lerp(double t, double a, double b) {
//    return a + t * (b - a);
//}
//
//__device__ double grad(int hash, double x, double y, double z) {
//    int h = hash & 15;                      // CONVERT LO 4 BITS OF HASH CODE
//    double u = h < 8 ? x : y,               // INTO 12 GRADIENT DIRECTIONS.
//        v = h < 4 ? y : h == 12 || h == 14 ? x : z;
//    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
//}


__device__ float grad(int hash, float x, float y, float z);
__device__ float lerp(float t, float a, float b);
__device__ float fade(float t);

__device__ float getNoiseAt(float x, float y, float z, int* p)
{
    int X = (int)floor(x) & 255;
    int Y = (int)floor(y) & 255;
    int Z = (int)floor(z) & 255;

    x -= floor(x);
    y -= floor(y);
    z -= floor(z);

    float u = fade(x);
    float v = fade(y);
    float w = fade(z);

    int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z,      // HASH COORDINATES OF
        B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;      // THE 8 CUBE CORNERS,

    //printf("x: %f y: %f z: %f X: %d Y: %d Z: %d u: %f v: %f w: %f A: %d AA: %d AB: %d B: %d BA: %d BB: %d", x, y, z, X, Y, Z, u, v, w, A, AA, AB, B, BA, BB);

    return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),  // AND ADD
        grad(p[BA], x - 1, y, z)), // BLENDED
        lerp(u, grad(p[AB], x, y - 1, z),  // RESULTS
            grad(p[BB], x - 1, y - 1, z))),// FROM  8
        lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),  // CORNERS
            grad(p[BA + 1], x - 1, y, z - 1)), // OF CUBE
            lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
                grad(p[BB + 1], x - 1, y - 1, z - 1))));
}

__device__ float getPerlinNoiseAt(float x, float y, float z, int* p) {
    return (float)(getNoiseAt(x, y, z, p) * FACTOR_3D);
}


__device__ float fade(float t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
}

__device__ float lerp(float t, float a, float b) {
    return a + t * (b - a);
}

__device__ float grad(int hash, float x, float y, float z) {
    int h = hash & 15;                      // CONVERT LO 4 BITS OF HASH CODE
    float u = h < 8 ? x : y,               // INTO 12 GRADIENT DIRECTIONS.
        v = h < 4 ? y : h == 12 || h == 14 ? x : z;
    return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
}
