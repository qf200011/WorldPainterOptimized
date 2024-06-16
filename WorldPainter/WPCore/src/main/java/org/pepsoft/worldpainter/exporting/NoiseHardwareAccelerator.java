package org.pepsoft.worldpainter.exporting;

import org.pepsoft.minecraft.Material;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public final class NoiseHardwareAccelerator {
    static {
        try {
            System.load("C:\\Development\\WorldPainterGPU\\WorldPainter\\NoiseHardwareAccelerator\\x64\\Debug\\NoiseHardwareAccelerator.dll");
            System.out.println("Library loaded successfully!");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load. \n" + e);
        }
    }

    private static NoiseHardwareAccelerator instance;

    public AvailableGPUMemoryController availableGPUMemoryController;

    private NoiseHardwareAccelerator(){
        this.calculatedNoises=new HashMap<>();
        this.regionExporters = new HashMap<>();
        this.availableGPUMemoryController=new AvailableGPUMemoryController();
    }

    private class AvailableGPUMemoryController{
        private ArrayList<Boolean> isMemoryAvailableList;

        //array of size of number of gpu threads that contains an array of pointers to GPU memory. One for each material.
        private ArrayList<ArrayList<Long>> xRegionMaterialGPUPointerArray;
        private ArrayList<ArrayList<Long>> yRegionMaterialGPUPointerArray;
        private ArrayList<ArrayList<Long>> zRegionMaterialGPUPointerArray;
        private ArrayList<ArrayList<Long>> pGPUPointerArray;
        private ArrayList<ArrayList<Long>> outputGPUPointerArray;

        private ArrayList<Boolean> isMemoryAvailableArray;
        private HashMap<Long, Integer> threadIdToMemoryIndexMap;


        public AvailableGPUMemoryController(){
            isMemoryAvailableArray=new ArrayList<>(gpuThreads);
            threadIdToMemoryIndexMap=new HashMap<>();

            xRegionMaterialGPUPointerArray= new ArrayList<>(gpuThreads);
            yRegionMaterialGPUPointerArray= new ArrayList<>(gpuThreads);
            zRegionMaterialGPUPointerArray= new ArrayList<>(gpuThreads);
            pGPUPointerArray =new ArrayList<>(gpuThreads);
            outputGPUPointerArray =new ArrayList<>(gpuThreads);

            for (int i=0;i<gpuThreads;i++){
                isMemoryAvailableArray.add(true);

                xRegionMaterialGPUPointerArray.add(new ArrayList<>());
                yRegionMaterialGPUPointerArray.add(new ArrayList<>());
                zRegionMaterialGPUPointerArray.add(new ArrayList<>());
                pGPUPointerArray.add(new ArrayList<>());
                outputGPUPointerArray.add(new ArrayList<>());
            }


        }

        public long getXGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.xRegionMaterialGPUPointerArray);

            return this.xRegionMaterialGPUPointerArray.get(index).get(materialIndex);
        }

        public long getYGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.yRegionMaterialGPUPointerArray);

            return yRegionMaterialGPUPointerArray.get(index).get(materialIndex);
        }

        public long getZGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.zRegionMaterialGPUPointerArray);

            return this.zRegionMaterialGPUPointerArray.get(index).get(materialIndex);
        }

        public long getPGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.pGPUPointerArray);

            return this.pGPUPointerArray.get(index).get(materialIndex);
        }

        public long getOutputGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.outputGPUPointerArray);

            return this.outputGPUPointerArray.get(index).get(materialIndex);
        }

        private synchronized int getMemoryIndex(long threadId, ArrayList<ArrayList<Long>> gpuPointerArray){
            Integer memoryIndex= this.threadIdToMemoryIndexMap.get(threadId);

            int index = -1;
            if (memoryIndex!=null){
                index=memoryIndex;
            }
            else {
                for (int i = 0; i < gpuThreads; i++) {
                    if (this.isMemoryAvailableArray.get(i)) {
                        this.threadIdToMemoryIndexMap.put(threadId, i);
                        this.isMemoryAvailableArray.set(i, false);
                        index = i;
                        break;
                    }
                }
            }

            if (gpuPointerArray.get(index).size()!=12) {
                for (int i = 0; i < 12; i++) { //todo get actual material count
                    gpuPointerArray.get(index).add(0L);
                }
            }



            return index;
        }

        public void freeMemoryIndex(long threadId){
            Integer memoryIndex = this.threadIdToMemoryIndexMap.get(threadId);
            if (memoryIndex==null) return;

            this.isMemoryAvailableArray.set(memoryIndex,true);
            this.threadIdToMemoryIndexMap.remove(threadId);
        }

        public void setGPUMemoryPointers(long threadId, int materialIndex,NoiseHardwareAcceleratorResponse response){
            if (this.threadIdToMemoryIndexMap.containsKey(threadId)){
                int memoryIndex= this.threadIdToMemoryIndexMap.get(threadId);
                this.xRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getxRegionGPUPointer());
                this.yRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getyRegionGPUPointer());
                this.zRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getzRegionGPUPointer());
                this.pGPUPointerArray.get(memoryIndex).set(materialIndex, response.getpRegionGPUPointer());
                this.outputGPUPointerArray.get(memoryIndex).set(materialIndex,response.getOutputRegionGPUPointer());
                return;
            }

            for (int memoryIndex =0;memoryIndex<gpuThreads; memoryIndex++){
                if (this.isMemoryAvailableArray.get(memoryIndex)){
                    this.threadIdToMemoryIndexMap.put(threadId,memoryIndex);
                    this.isMemoryAvailableArray.set(memoryIndex,false);

                    this.xRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getxRegionGPUPointer());
                    this.yRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getyRegionGPUPointer());
                    this.zRegionMaterialGPUPointerArray.get(memoryIndex).set(materialIndex, response.getzRegionGPUPointer());
                    this.pGPUPointerArray.get(memoryIndex).set(materialIndex, response.getpRegionGPUPointer());
                    this.outputGPUPointerArray.get(memoryIndex).set(materialIndex, response.getOutputRegionGPUPointer());
                }
            }
        }


    }

    public NoiseHardwareAcceleratorRequest getNoiseHardwareRequest(Point regionCoords, long threadId, int materialIndex){
        long xRegionArrayPointer=this.availableGPUMemoryController.getXGPUMemoryPointer(threadId,materialIndex);
        long yRegionArrayPointer=this.availableGPUMemoryController.getYGPUMemoryPointer(threadId,materialIndex);
        long zRegionArrayPointer=this.availableGPUMemoryController.getZGPUMemoryPointer(threadId,materialIndex);
        long pArrayPointer=this.availableGPUMemoryController.getPGPUMemoryPointer(threadId,materialIndex);
        long outputArrayPointer=this.availableGPUMemoryController.getOutputGPUMemoryPointer(threadId,materialIndex);

        ResourcesExporter resourcesExporter = this.regionExporters.get(regionCoords);
        if (resourcesExporter==null){
            return null;
        }

        long materialSeed=resourcesExporter.noiseGenerators[materialIndex].getSeed();
        int materialMaxHeight = resourcesExporter.maxLevels[materialIndex];
        int materialMinHeight= resourcesExporter.minLevels[materialIndex];
       ResourcesExporter.ResourcesExporterSettings resourceSettings= (ResourcesExporter.ResourcesExporterSettings) resourcesExporter.settings;

       float[] materialChances = new float[16];

       for (int i=0; i<16; i++){
               materialChances[i] = PerlinNoise.getLevelForPromillage(Math.min(resourceSettings.getChance(resourcesExporter.activeMaterials[materialIndex]) * i / 8f, 1000f));
       }

        ByteBuffer output = ByteBuffer.allocateDirect(680*680*(materialMaxHeight-materialMinHeight)); //one bit for each block to determine if it should or shouldn't place a block of a material type.

        return new NoiseHardwareAcceleratorRequest(materialSeed,regionCoords.x,regionCoords.y,materialMinHeight, materialMaxHeight,xRegionArrayPointer,yRegionArrayPointer,zRegionArrayPointer,pArrayPointer,outputArrayPointer,output,materialChances);
    }

    public static NoiseHardwareAccelerator getInstance(){
        if (instance==null) {
            instance = new NoiseHardwareAccelerator();
        }
        return instance;
    }

    public final static boolean isGPUEnabled =true;

    public final  static int gpuThreads =8;

    public native static NoiseHardwareAcceleratorResponse getRegionNoiseData(NoiseHardwareAcceleratorRequest request);

    public HashMap<Point,HashMap<Integer,ByteBuffer>> calculatedNoises;

    private HashMap<Point, ResourcesExporter> regionExporters;

    public synchronized  boolean allocateSpot(Point regionCoords, ResourcesExporter resourcesExporter){
        if (this.regionExporters.size()>=gpuThreads){
            return false;
        }

        this.regionExporters.put(regionCoords,resourcesExporter);
        return true;
    }

    public synchronized  void freeSpot(Point regionCoords, long threadId){
        this.calculatedNoises.remove(regionCoords);
        this.regionExporters.remove(regionCoords);

        this.availableGPUMemoryController.freeMemoryIndex(threadId);
    }

    public void addCalculatedNoiseForRegion(Point regionCoords, int materialIndex, long threadId){

        if (this.calculatedNoises.size()>gpuThreads){ //do cpu instead //todo check if gpu memory is full instead
            return;
        }

        ResourcesExporter resourcesExporter = this.regionExporters.get(regionCoords);
        if (resourcesExporter==null){
            return;
        }

        NoiseHardwareAcceleratorRequest noiseHardwareAcceleratorRequest = this.getNoiseHardwareRequest(regionCoords,threadId,materialIndex);

        NoiseHardwareAcceleratorResponse response=NoiseHardwareAccelerator.getRegionNoiseData(noiseHardwareAcceleratorRequest);

        ByteBuffer outputByteBuffer=noiseHardwareAcceleratorRequest.getOutputArray();

        if (outputByteBuffer==null){
            return;
        }

        this.availableGPUMemoryController.setGPUMemoryPointers(threadId,materialIndex,response);

        this.setCalculatedNoises(materialIndex,regionCoords,outputByteBuffer);
    }

    private synchronized void setCalculatedNoises(Integer materialIndex,Point regionCoords, ByteBuffer outputByteBuffer){
        if (this.calculatedNoises.containsKey(regionCoords)){
            this.calculatedNoises.get(regionCoords).put(materialIndex, outputByteBuffer);
        }
        else{
            HashMap<Integer, ByteBuffer> innerMap = new HashMap<Integer, ByteBuffer>();
            innerMap.put(materialIndex, outputByteBuffer);
            this.calculatedNoises.put(regionCoords, innerMap);
        }
    }
}
