package org.pepsoft.worldpainter.exporting.gpuacceleration;

public class GPUMemoryBlock{
    //array of size of number of gpu memory allocations that contains an array of pointers to GPU memory.
    private long xGPUPointer;
    private long yGPUPointer;
    private long zGPUPointer;
    private long pGPUPointer;
    private long outputGPUPointer;
    private long compactedOutputGPUPointer;

    public GPUMemoryBlock(long xGPUPointer, long yGPUPointer, long zGPUPointer, long pGPUPointer, long outputGPUPointer, long compactedOutputGPUPointer) {
        this.xGPUPointer = xGPUPointer;
        this.yGPUPointer = yGPUPointer;
        this.zGPUPointer = zGPUPointer;
        this.pGPUPointer = pGPUPointer;
        this.outputGPUPointer = outputGPUPointer;
        this.compactedOutputGPUPointer = compactedOutputGPUPointer;
    }

    public void setxGPUPointer(long xGPUPointer) {
        this.xGPUPointer = xGPUPointer;
    }

    public void setyGPUPointer(long yGPUPointer) {
        this.yGPUPointer = yGPUPointer;
    }

    public void setzGPUPointer(long zGPUPointer) {
        this.zGPUPointer = zGPUPointer;
    }

    public void setpGPUPointer(long pGPUPointer) {
        this.pGPUPointer = pGPUPointer;
    }

    public void setOutputGPUPointer(long outputGPUPointer) {
        this.outputGPUPointer = outputGPUPointer;
    }

    public void setCompactedOutputGPUPointer(long compactedOutputGPUPointer) {
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
