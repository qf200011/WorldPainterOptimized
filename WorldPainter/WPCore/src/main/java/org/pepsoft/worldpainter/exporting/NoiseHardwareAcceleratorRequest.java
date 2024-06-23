package org.pepsoft.worldpainter.exporting;

import java.nio.ByteBuffer;

public class NoiseHardwareAcceleratorRequest {

    //request info
    private long materialSeed;
    private int regionX;
    private int regionY; //y direction on a map, not in game
    private int materialMinHeight;
    private int materialMaxHeight;

    //gpu pointers
    private long regionXPtr;
    private long regionYPtr;
    private long regionZPtr;
    private long pPtr;
    private long outputPtr;
    private long compactedOutputPtr;
    private float[] chances;


    public NoiseHardwareAcceleratorRequest(long materialSeed, int regionX, int regionY, int materialMinHeight, int materialMaxHeight, long regionXPtr, long regionYPtr, long regionZPtr, long pPtr, long outputPtr, long compactedOutputPtr, float[] chances) {
        this.materialSeed = materialSeed;
        this.regionX = regionX;
        this.regionY = regionY;
        this.materialMinHeight = materialMinHeight;
        this.materialMaxHeight = materialMaxHeight;
        this.regionXPtr = regionXPtr;
        this.regionYPtr = regionYPtr;
        this.regionZPtr = regionZPtr;
        this.pPtr = pPtr;
        this.outputPtr = outputPtr;
        this.compactedOutputPtr=compactedOutputPtr;
        this.chances=chances;
    }

    public long getMaterialSeed() {
        return materialSeed;
    }

    public void setMaterialSeed(long materialSeed) {
        this.materialSeed = materialSeed;
    }

    public int getRegionX() {
        return regionX;
    }

    public void setRegionX(int regionX) {
        this.regionX = regionX;
    }

    public int getRegionY() {
        return regionY;
    }

    public void setRegionY(int regionY) {
        this.regionY = regionY;
    }

    public int getMaterialMinHeight() {
        return materialMinHeight;
    }

    public void setMaterialMinHeight(int materialMinHeight) {
        this.materialMinHeight = materialMinHeight;
    }

    public int getMaterialMaxHeight() {
        return materialMaxHeight;
    }

    public void setMaterialMaxHeight(int materialMaxHeight) {
        this.materialMaxHeight = materialMaxHeight;
    }

    public long getRegionXPtr() {
        return regionXPtr;
    }

    public void setRegionXPtr(long regionXPtr) {
        this.regionXPtr = regionXPtr;
    }

    public long getRegionYPtr() {
        return regionYPtr;
    }

    public void setRegionYPtr(long regionYPtr) {
        this.regionYPtr = regionYPtr;
    }

    public long getRegionZPtr() {
        return regionZPtr;
    }

    public void setRegionZPtr(long regionZPtr) {
        this.regionZPtr = regionZPtr;
    }

    public long getpPtr() {
        return pPtr;
    }

    public void setpPtr(long pPtr) {
        this.pPtr = pPtr;
    }

    public long getOutputPtr() {
        return outputPtr;
    }

    public void setOutputPtr(long outputPtr) {
        this.outputPtr = outputPtr;
    }

    public long getCompactedOutputPtr() {
        return compactedOutputPtr;
    }

    public void setCompactedOutputPtr(long compactedOutputPtr) {
        this.compactedOutputPtr = compactedOutputPtr;
    }

    public float[] getChances() {
        return chances;
    }

    public void setChances(float[] chances) {
        this.chances = chances;
    }
}
