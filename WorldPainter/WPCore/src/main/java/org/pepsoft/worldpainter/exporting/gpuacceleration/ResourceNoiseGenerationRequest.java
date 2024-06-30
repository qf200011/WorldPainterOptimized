package org.pepsoft.worldpainter.exporting.gpuacceleration;

import org.pepsoft.worldpainter.exporting.NoiseHardwareAccelerator;
import org.pepsoft.worldpainter.exporting.NoiseHardwareAcceleratorResponse;

public class ResourceNoiseGenerationRequest extends NoiseGenerationRequest {
    private final float[] chances;

    public ResourceNoiseGenerationRequest(long seed, int regionX, int regionY, int minHeight,int maxHeight, int heightOffset, float blobSize,GPUOptimizable gpuOptimizable, float[] chances) {
        super(seed, regionX, regionY, minHeight, maxHeight,heightOffset, blobSize, gpuOptimizable);
        this.chances = chances;
    }




    private native NoiseHardwareAcceleratorResponse getResourceRegionNoiseData(NoiseHardwareAccelerator.GPUNoiseRequest request);

    public float[] getChances() {
        return chances;
    }

    @Override
    public NoiseHardwareAcceleratorResponse getRegionNoiseData(NoiseHardwareAccelerator.GPUNoiseRequest request) {
        return getResourceRegionNoiseData(request);
    }
}
