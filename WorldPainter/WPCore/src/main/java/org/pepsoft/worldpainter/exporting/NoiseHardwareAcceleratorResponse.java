package org.pepsoft.worldpainter.exporting;

public class NoiseHardwareAcceleratorResponse {
    private int[] output;
    private  long xRegionGPUPointer;
    private  long yRegionGPUPointer;
    private  long zRegionGPUPointer;
    private  long pRegionGPUPointer;
    private  long outputRegionGPUPointer;
    private long compactedOutputGPUPointer;

    public NoiseHardwareAcceleratorResponse(int[] output, long xRegionGPUPointer, long yRegionGPUPointer, long zRegionGPUPointer, long pRegionGPUPointer, long outputRegionGPUPointer, long compactedOutputGPUPointer){
        this.output=output;
        this.xRegionGPUPointer=xRegionGPUPointer;
        this.yRegionGPUPointer=yRegionGPUPointer;
        this.zRegionGPUPointer=zRegionGPUPointer;
        this.pRegionGPUPointer=pRegionGPUPointer;
        this.outputRegionGPUPointer=outputRegionGPUPointer;
        this.compactedOutputGPUPointer=compactedOutputGPUPointer;
    }

    public int[] getOutput() {
        return output;
    }

    public void setOutput(int[] output) {
        this.output = output;
    }

    public long getxRegionGPUPointer() {
        return xRegionGPUPointer;
    }

    public void setxRegionGPUPointer(long xRegionGPUPointer) {
        this.xRegionGPUPointer = xRegionGPUPointer;
    }

    public long getyRegionGPUPointer() {
        return yRegionGPUPointer;
    }

    public void setyRegionGPUPointer(long yRegionGPUPointer) {
        this.yRegionGPUPointer = yRegionGPUPointer;
    }

    public long getzRegionGPUPointer() {
        return zRegionGPUPointer;
    }

    public void setzRegionGPUPointer(long zRegionGPUPointer) {
        this.zRegionGPUPointer = zRegionGPUPointer;
    }

    public long getpRegionGPUPointer() {
        return pRegionGPUPointer;
    }

    public void setpRegionGPUPointer(long pRegionGPUPointer) {
        this.pRegionGPUPointer = pRegionGPUPointer;
    }

    public long getOutputRegionGPUPointer() {
        return outputRegionGPUPointer;
    }

    public void setOutputRegionGPUPointer(long outputRegionGPUPointer) {
        this.outputRegionGPUPointer = outputRegionGPUPointer;
    }

    public long getCompactedOutputGPUPointer() {
        return compactedOutputGPUPointer;
    }

    public void setCompactedOutputGPUPointer(long compactedOutputGPUPointer) {
        this.compactedOutputGPUPointer = compactedOutputGPUPointer;
    }
}
