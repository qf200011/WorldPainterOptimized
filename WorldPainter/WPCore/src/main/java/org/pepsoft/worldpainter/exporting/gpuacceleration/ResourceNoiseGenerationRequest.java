package org.pepsoft.worldpainter.exporting.gpuacceleration;

import org.pepsoft.worldpainter.exporting.NoiseHardwareAcceleratorResponse;

public class ResourceNoiseGenerationRequest extends NoiseGenerationRequest {
    private final float[] chances;

    public ResourceNoiseGenerationRequest(long seed, int regionX, int regionY, int startingHeight, float blobSize,GPUOptimizable gpuOptimizable, float[] chances) {
        super(seed, regionX, regionY, startingHeight, blobSize, gpuOptimizable);
        this.chances = chances;
    }




    private native NoiseHardwareAcceleratorResponse getResourceRegionNoiseData(NoiseGenerationRequest request);

    public float[] getChances() {
        return chances;
    }

    @Override
    public NoiseHardwareAcceleratorResponse getRegionNoiseData() {
        return getResourceRegionNoiseData(this);
    }
}
