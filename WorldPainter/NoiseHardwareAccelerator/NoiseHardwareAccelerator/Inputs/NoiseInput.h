#pragma once

#include "../JavaClassWrappers/NoiseGenerationRequest.h"
#include "../JavaClassWrappers/JavaRandom.h"

class NoiseInput
{
private:
	float* regionArrayX;
	float* regionArrayY;
	float* regionArrayZ;
	int* pArray;

	const int X_ARRAY_SIZE = 512;
	const int Y_ARRAY_SIZE = 512;
	const int TILES_PER_REGION_AXIS = 4;
	const int TILE_SIZE = 128;
	const int CHUNKS_PER_TILE_AXIS = 8;
	const int CHUNK_SIZE = 16;


	void createRegionArrays(NoiseGenerationRequest noiseGenerationRequest);
	void createPArray(JavaRandom random);
	void swap(int* array, int index1, int index2);

public:
	NoiseInput(NoiseGenerationRequest noiseGenerationRequest, JavaRandom random);
	~NoiseInput();


	float* getRegionArrayX();
	float* getRegionArrayY();
	float* getRegionArrayZ();
	int* getPArray();
};

