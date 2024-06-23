package org.pepsoft.worldpainter.exporting.gpuacceleration;

public class GPUMemoryBlock{
    //array of size of number of gpu memory allocations that contains an array of pointers to GPU memory.
    private final Long xGPUPointer;
    private final Long yGPUPointer;
    private final Long zGPUPointer;
    private final Long pGPUPointer;
    private final Long outputGPUPointer;
    private final Long compactedGPUPointer;

    public GPUMemoryBlock(Long xGPUPointer, Long yGPUPointer, Long zGPUPointer, Long pGPUPointer, Long outputGPUPointer, Long compactedGPUPointer) {
        this.xGPUPointer = xGPUPointer;
        this.yGPUPointer = yGPUPointer;
        this.zGPUPointer = zGPUPointer;
        this.pGPUPointer = pGPUPointer;
        this.outputGPUPointer = outputGPUPointer;
        this.compactedGPUPointer = compactedGPUPointer;
    }

    public GPUMemoryBlock() {
        this(0L,0L,0L,0L,0L,0L);
    }

    public Long getxGPUPointer() {
        return xGPUPointer;
    }

    public Long getyGPUPointer() {
        return yGPUPointer;
    }

    public Long getzGPUPointer() {
        return zGPUPointer;
    }

    public Long getpGPUPointer() {
        return pGPUPointer;
    }

    public Long getOutputGPUPointer() {
        return outputGPUPointer;
    }

    public Long getCompactedGPUPointer() {
        return compactedGPUPointer;
    }
}
