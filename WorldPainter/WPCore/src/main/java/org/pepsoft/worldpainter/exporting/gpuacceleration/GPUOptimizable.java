package org.pepsoft.worldpainter.exporting.gpuacceleration;

import java.awt.*;

public interface GPUOptimizable  {
    public void computePerlinNoiseOnGPU(Point region);
    public void computeCallBack(long threadId, int processId, int[] outputIndexes);
}
