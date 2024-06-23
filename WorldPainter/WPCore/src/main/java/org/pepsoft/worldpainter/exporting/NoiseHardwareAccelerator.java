package org.pepsoft.worldpainter.exporting;

import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.layers.exporters.ResourcesExporter;

import java.awt.*;
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
        private ArrayList<ArrayList<Long>> xRegionGPUPointerArray;
        private ArrayList<ArrayList<Long>> yRegionGPUPointerArray;
        private ArrayList<ArrayList<Long>> zRegionGPUPointerArray;
        private ArrayList<ArrayList<Long>> pGPUPointerArray;
        private ArrayList<ArrayList<Long>> outputGPUPointerArray;
        private ArrayList<ArrayList<Long>> compactedGPUPointerArray;

        private ArrayList<Boolean> isMemoryAvailableArray;
        private HashMap<Long, Integer> threadIdToMemoryIndexMap;


        public AvailableGPUMemoryController(){
            isMemoryAvailableArray=new ArrayList<>(gpuThreads);
            threadIdToMemoryIndexMap=new HashMap<>();

            xRegionGPUPointerArray = new ArrayList<>(gpuThreads);
            yRegionGPUPointerArray = new ArrayList<>(gpuThreads);
            zRegionGPUPointerArray = new ArrayList<>(gpuThreads);
            pGPUPointerArray =new ArrayList<>(gpuThreads);
            outputGPUPointerArray =new ArrayList<>(gpuThreads);
            compactedGPUPointerArray =new ArrayList<>(gpuThreads);

            for (int i=0;i<gpuThreads;i++){
                isMemoryAvailableArray.add(true);

                xRegionGPUPointerArray.add(new ArrayList<>());
                yRegionGPUPointerArray.add(new ArrayList<>());
                zRegionGPUPointerArray.add(new ArrayList<>());
                pGPUPointerArray.add(new ArrayList<>());
                outputGPUPointerArray.add(new ArrayList<>());
                compactedGPUPointerArray.add(new ArrayList<>());
            }


        }

        public long getXGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.xRegionGPUPointerArray);

            return this.xRegionGPUPointerArray.get(index).get(materialIndex);
        }

        public long getYGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.yRegionGPUPointerArray);

            return yRegionGPUPointerArray.get(index).get(materialIndex);
        }

        public long getZGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.zRegionGPUPointerArray);

            return this.zRegionGPUPointerArray.get(index).get(materialIndex);
        }

        public long getPGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.pGPUPointerArray);

            return this.pGPUPointerArray.get(index).get(materialIndex);
        }

        public long getOutputGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.outputGPUPointerArray);

            return this.outputGPUPointerArray.get(index).get(materialIndex);
        }

        public long getCompactedOutputGPUMemoryPointer(long threadId, int materialIndex){
            int index=getMemoryIndex(threadId,this.compactedGPUPointerArray);

            return this.compactedGPUPointerArray.get(index).get(materialIndex);
        }

        private synchronized int getMemoryIndex(long threadId, ArrayList<ArrayList<Long>> gpuPointerArray){
            Integer memoryIndex= this.threadIdToMemoryIndexMap.get(threadId);

            if (gpuPointerArray.get(memoryIndex).size()!=12) {
                for (int i = 0; i < 12; i++) { //todo get actual material count
                    gpuPointerArray.get(memoryIndex).add(0L);
                }
            }



            return memoryIndex;
        }

        public boolean waitForOpenMemory(long threadId, int retries){
            int counter=0;
            while (counter<retries){
                counter++;

                int index=getOpenIndex(threadId);

                if (index!=-1){
                    return true;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return false;
        }

        private synchronized int getOpenIndex(long threadId){
            for (int i=0; i<this.isMemoryAvailableArray.size(); i++){
                if (this.isMemoryAvailableArray.get(i)) {
                    this.threadIdToMemoryIndexMap.put(threadId, i);
                    this.isMemoryAvailableArray.set(i, false);
                    return i;
                }
            }
            return -1;
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
                this.xRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getxRegionGPUPointer());
                this.yRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getyRegionGPUPointer());
                this.zRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getzRegionGPUPointer());
                this.pGPUPointerArray.get(memoryIndex).set(materialIndex, response.getpRegionGPUPointer());
                this.outputGPUPointerArray.get(memoryIndex).set(materialIndex,response.getOutputRegionGPUPointer());
                this.compactedGPUPointerArray.get(memoryIndex).set(materialIndex,response.getCompactedOutputGPUPointer());
                return;
            }

            for (int memoryIndex =0;memoryIndex<gpuThreads; memoryIndex++){
                if (this.isMemoryAvailableArray.get(memoryIndex)){
                    this.threadIdToMemoryIndexMap.put(threadId,memoryIndex);
                    this.isMemoryAvailableArray.set(memoryIndex,false);

                    this.xRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getxRegionGPUPointer());
                    this.yRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getyRegionGPUPointer());
                    this.zRegionGPUPointerArray.get(memoryIndex).set(materialIndex, response.getzRegionGPUPointer());
                    this.pGPUPointerArray.get(memoryIndex).set(materialIndex, response.getpRegionGPUPointer());
                    this.outputGPUPointerArray.get(memoryIndex).set(materialIndex, response.getOutputRegionGPUPointer());
                    this.compactedGPUPointerArray.get(memoryIndex).set(materialIndex, response.getCompactedOutputGPUPointer());
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
        long compactedOutputArrayPointer = this.availableGPUMemoryController.getCompactedOutputGPUMemoryPointer(threadId,materialIndex);

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

        return new NoiseHardwareAcceleratorRequest(materialSeed,regionCoords.x,regionCoords.y,materialMinHeight, materialMaxHeight,xRegionArrayPointer,yRegionArrayPointer,zRegionArrayPointer,pArrayPointer,outputArrayPointer,compactedOutputArrayPointer,materialChances);
    }

    public static NoiseHardwareAccelerator getInstance(){
        if (instance==null) {
            instance = new NoiseHardwareAccelerator();
        }
        return instance;
    }

    public final static boolean isGPUEnabled =true;

    public final  static int gpuThreads =4;

    public native static NoiseHardwareAcceleratorResponse getRegionNoiseData(NoiseHardwareAcceleratorRequest request);

    public HashMap<Point,HashMap<Integer,int[]>> calculatedNoises;

    private HashMap<Point, ResourcesExporter> regionExporters;

    public boolean allocateSpot(Point regionCoords, long threadId, ResourcesExporter resourcesExporter){
        if (!this.availableGPUMemoryController.waitForOpenMemory(threadId, 10)){
            return false;
        }

        this.takeReservedSpot(regionCoords,resourcesExporter);
        return true;
    }

    private synchronized void takeReservedSpot(Point regionCoords, ResourcesExporter resourcesExporter){
        this.regionExporters.put(regionCoords,resourcesExporter);
    }

    public synchronized  void freeSpot(Point regionCoords){
        this.calculatedNoises.remove(regionCoords);
        this.regionExporters.remove(regionCoords);


    }

    public synchronized void freeMemory(long threadId){
        this.availableGPUMemoryController.freeMemoryIndex(threadId);
    }

    public void addCalculatedNoiseForRegion(Point regionCoords, int materialIndex, long threadId){

        ResourcesExporter resourcesExporter = this.regionExporters.get(regionCoords);
        if (resourcesExporter==null){
            return;
        }

        NoiseHardwareAcceleratorRequest noiseHardwareAcceleratorRequest = this.getNoiseHardwareRequest(regionCoords,threadId,materialIndex);

        NoiseHardwareAcceleratorResponse response=NoiseHardwareAccelerator.getRegionNoiseData(noiseHardwareAcceleratorRequest);

        int[] outputIndexes= response.getOutput();

        if (outputIndexes==null){
            return;
        }

        this.availableGPUMemoryController.setGPUMemoryPointers(threadId,materialIndex,response);

        this.setCalculatedNoises(materialIndex,regionCoords,outputIndexes);
    }

    private synchronized void setCalculatedNoises(Integer materialIndex,Point regionCoords, int[] outputIndexes){
        if (this.calculatedNoises.containsKey(regionCoords)){
            this.calculatedNoises.get(regionCoords).put(materialIndex, outputIndexes);
        }
        else{
            HashMap<Integer, int[]> innerMap = new HashMap<Integer, int[]>();
            innerMap.put(materialIndex, outputIndexes);
            this.calculatedNoises.put(regionCoords, innerMap);
        }
    }
}
