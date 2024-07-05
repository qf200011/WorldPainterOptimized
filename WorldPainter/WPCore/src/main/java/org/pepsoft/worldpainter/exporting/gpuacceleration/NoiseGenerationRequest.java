package org.pepsoft.worldpainter.exporting.gpuacceleration;

import org.pepsoft.worldpainter.exporting.NoiseHardwareAccelerator;
import org.pepsoft.worldpainter.exporting.NoiseHardwareAcceleratorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NoiseGenerationRequest {

    private static final Logger log = LoggerFactory.getLogger(NoiseGenerationRequest.class);
    //request info
    private final int regionX;
    private final int regionY; //y direction on a map, not in game
    private final int minHeight;
    private final int maxHeight;
    private final float blobSize;

    private final GPUOptimizableExporter gpuOptimizableExporter;

    public static final int HEIGHT_SIZE=32;


    public NoiseGenerationRequest( int regionX, int regionY, int minHeight, int maxHeight, float blobSize, GPUOptimizableExporter gpuOptimizableExporter) {
        this.regionX = regionX;
        this.regionY = regionY;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.blobSize=blobSize;
        this.gpuOptimizableExporter = gpuOptimizableExporter;
    }

    public abstract NoiseHardwareAcceleratorResponse getRegionNoiseData(NoiseHardwareAccelerator.GPUNoiseRequest request);

    public int getRegionX() {
        return regionX;
    }

    public int getRegionY() {
        return regionY;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public float getBlobSize() {
        return blobSize;
    }

    public void executeCallback(long threadId, int processId, int[] outputIndexes){
        gpuOptimizableExporter.computeCallBack(threadId,processId,outputIndexes);
    }
}
