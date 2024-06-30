package org.pepsoft.worldpainter.exporting.gpuacceleration;

import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.exporting.MinecraftWorld;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public interface GPUOptimizable  {
    public void computePerlinNoiseOnGPU(Point region, HashMap<Point, Tile> tileCoordsToTiles);
    public void computeCallBack(long threadId, int processId, int[] outputIndexes);
    public void renderAll(MinecraftWorld world, Map<Point,Tile> tiles, Point regionCoords);
}
