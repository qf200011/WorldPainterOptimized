package org.pepsoft.worldpainter.exporting.gpuacceleration;

public class GPUMemoryBlock{
    //array of size of number of gpu memory allocations that contains an array of pointers to GPU memory.
    private final long xGPUPointer;
    private final long yGPUPointer;
    private final long zGPUPointer;
    private final long pGPUPointer;
    private final long outputGPUPointer;
    private final long compactedOutputGPUPointer;

    public GPUMemoryBlock(long xGPUPointer, long yGPUPointer, long zGPUPointer, long pGPUPointer, long outputGPUPointer, long compactedOutputGPUPointer) {
        this.xGPUPointer = xGPUPointer;
        this.yGPUPointer = yGPUPointer;
        this.zGPUPointer = zGPUPointer;
        this.pGPUPointer = pGPUPointer;
        this.outputGPUPointer = outputGPUPointer;
        this.compactedOutputGPUPointer = compactedOutputGPUPointer;
    }

    public GPUMemoryBlock() {
        this(0L,0L,0L,0L,0L,0L);
    }

    public long getxGPUPointer() {
        return xGPUPointer;
    }

    public long getyGPUPointer() {
        return yGPUPointer;
    }

    public long getzGPUPointer() {
        return zGPUPointer;
    }

    public long getpGPUPointer() {
        return pGPUPointer;
    }

    public long getOutputGPUPointer() {
        return outputGPUPointer;
    }

    public long getCompactedOutputGPUPointer() {
        return compactedOutputGPUPointer;
    }
}
