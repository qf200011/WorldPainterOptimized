#include "NoiseInput.h"

NoiseInput::NoiseInput(NoiseGenerationRequest noiseGenerationRequest, JavaRandom random)
{
    createRegionArrays(noiseGenerationRequest);
    createPArray(random);
}

NoiseInput::~NoiseInput() {
    delete[] regionArrayX;
    delete[] regionArrayY;
    delete[] regionArrayZ;
    delete[] pArray;

    regionArrayX = nullptr;
    regionArrayY = nullptr;
    regionArrayZ = nullptr;
    pArray = nullptr;
}

void NoiseInput::createRegionArrays(NoiseGenerationRequest noiseGenerationRequest) {
    int minTileX = noiseGenerationRequest.getRegionX() * 4;
    int minTileY = noiseGenerationRequest.getRegionY() * 4;

    int totalHeight = noiseGenerationRequest.getMaxHeight() - noiseGenerationRequest.getMinHeight();

    regionArrayX = new float[X_ARRAY_SIZE];
    regionArrayY = new float[Y_ARRAY_SIZE];
    regionArrayZ = new float[totalHeight];


    for (int tileX = 0; tileX < TILES_PER_REGION_AXIS; tileX++) {
        for (int x = 0; x < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; x++) {
            int worldX = (tileX + minTileX) * TILE_SIZE + x;
            regionArrayX[tileX * TILE_SIZE + x] = worldX / noiseGenerationRequest.getBlobSize();
        }
    }

    for (int tileY = 0; tileY < TILES_PER_REGION_AXIS; tileY++) {
        for (int y = 0; y < CHUNK_SIZE * CHUNKS_PER_TILE_AXIS; y++) {
            int worldY = (tileY + minTileY) * TILE_SIZE + y;
            regionArrayY[tileY * TILE_SIZE + y] = worldY / noiseGenerationRequest.getBlobSize();
        }
    }


    for (int z = 0; z < totalHeight; z++) {
        regionArrayZ[z] = (z + noiseGenerationRequest.getMinHeight()) / noiseGenerationRequest.getBlobSize();
    }
}

void NoiseInput::createPArray(JavaRandom random) {
    int permutation[256];

    for (int i = 0; i < 256; i++) {
        permutation[i] = i;
    }

    for (int i = 256; i > 1; i--) {
        swap(permutation, i - 1, random.nextInt(i));
    }

    pArray = new int[512];

    for (int i = 0; i < 256; i++) {
        pArray[256 + i] = pArray[i] = permutation[i];
    }
}

void NoiseInput::swap(int* array, int index1, int index2) {
    int temp = array[index1];
    array[index1] = array[index2];
    array[index2] = temp;
}

float* NoiseInput::getRegionArrayX() {
	return regionArrayX;
}

float* NoiseInput::getRegionArrayY() {
	return regionArrayY;
}

float* NoiseInput::getRegionArrayZ() {
	return regionArrayZ;
}

int* NoiseInput::getPArray() {
	return pArray;
}