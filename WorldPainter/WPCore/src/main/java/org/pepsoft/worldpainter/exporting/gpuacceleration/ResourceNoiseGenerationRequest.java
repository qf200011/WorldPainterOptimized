package org.pepsoft.worldpainter.exporting.gpuacceleration;

import org.pepsoft.worldpainter.exporting.NoiseHardwareAccelerator;
import org.pepsoft.worldpainter.exporting.NoiseHardwareAcceleratorResponse;

public class ResourceNoiseGenerationRequest extends NoiseGenerationRequest {
    private final float[] chances;
    private final long seed;

    public ResourceNoiseGenerationRequest(long seed, int regionX, int regionY, int minHeight,int maxHeight, float blobSize,GPUOptimizable gpuOptimizable, float[] chances) {
        super(regionX, regionY, minHeight, maxHeight, blobSize, gpuOptimizable);
        this.chances = chances;
        this.seed=seed;
    }




    private native NoiseHardwareAcceleratorResponse getResourceRegionNoiseData(NoiseHardwareAccelerator.GPUNoiseRequest request);

    public float[] getChances() {
        return chances;
    }

    public long getSeed() {
        return seed;
    }

    @Override
    public NoiseHardwareAcceleratorResponse getRegionNoiseData(NoiseHardwareAccelerator.GPUNoiseRequest request) {
        return getResourceRegionNoiseData(request);
    }
}
