package org.pepsoft.worldpainter.exporting;

import org.pepsoft.worldpainter.exporting.gpuacceleration.GPUMemoryBlock;

public class NoiseHardwareAcceleratorResponse {
    private final int[] output;
    private final GPUMemoryBlock gpuMemoryBlock;

    public NoiseHardwareAcceleratorResponse(int[] output, GPUMemoryBlock gpuMemoryBlock){
        this.output=output;
        this.gpuMemoryBlock=gpuMemoryBlock;
    }

    public int[] getOutput() {
        return output;
    }

    public GPUMemoryBlock getGpuMemoryBlock() {
        return gpuMemoryBlock;
    }
}
