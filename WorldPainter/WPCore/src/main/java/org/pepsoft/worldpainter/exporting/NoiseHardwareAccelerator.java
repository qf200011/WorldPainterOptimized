package org.pepsoft.worldpainter.exporting;

import org.jetbrains.annotations.NotNull;
import org.pepsoft.util.mdc.MDCThreadPoolExecutor;
import org.pepsoft.worldpainter.exporting.gpuacceleration.GPUMemoryBlock;
import org.pepsoft.worldpainter.exporting.gpuacceleration.NoiseGenerationRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

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
    private final GPUMemoryController gpuMemoryController;
    private final ExecutorService gpuExecutorService;
    private final ConcurrentLinkedQueue<NoiseRequestQueueEntry> noiseRequestQueue;

    private NoiseHardwareAccelerator(){
        gpuMemoryController =new GPUMemoryController();
        noiseRequestQueue = new ConcurrentLinkedQueue<>();
        gpuExecutorService=createGPUExecutorService("Calculating Noise",GPU_MEMORY_ALLOCATIONS);

        if (isGPUEnabled) {
            startGPUThreadPool();
        }
    }

    private ExecutorService createGPUExecutorService(String operation, int jobCount){
        return MDCThreadPoolExecutor.newFixedThreadPool(jobCount, new ThreadFactory() {
            @Override
            public synchronized Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread((threadGroup), r, operation.toLowerCase().replaceAll("\\s+", "-") + "-" + nextID++);
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }

            private final ThreadGroup threadGroup = new ThreadGroup(operation);
            private int nextID = 1;
        });
    }


    public class GPUNoiseRequest{
        private final NoiseGenerationRequest noiseGenerationRequest;
        private final GPUMemoryBlock gpuMemoryBlock;
        private final long threadId;
        private final int processId;

        private GPUNoiseRequest(NoiseGenerationRequest noiseGenerationRequest, GPUMemoryBlock gpuMemoryBlock, long threadId, int processId) {
            this.noiseGenerationRequest = noiseGenerationRequest;
            this.gpuMemoryBlock = gpuMemoryBlock;
            this.threadId=threadId;
            this.processId=processId;
        }

        private NoiseHardwareAcceleratorResponse execute(){
            NoiseHardwareAcceleratorResponse noiseHardwareAcceleratorResponse= this.noiseGenerationRequest.getRegionNoiseData(this);
            int[] outputIndexes = noiseHardwareAcceleratorResponse.getOutput();
            this.noiseGenerationRequest.executeCallback(threadId,processId,outputIndexes);
            return noiseHardwareAcceleratorResponse;
        }

        private int getProcessId() {
            return processId;
        }

        private long getThreadId() {
            return threadId;
        }

        public GPUMemoryBlock getGpuMemoryBlock() {
            return gpuMemoryBlock;
        }

        public NoiseGenerationRequest getNoiseGenerationRequest() {
            return noiseGenerationRequest;
        }
    }

    private class NoiseRequestQueueEntry{
        private final long threadId;
        private final int processId;
        private final NoiseGenerationRequest noiseGenerationRequest;

        public NoiseRequestQueueEntry(long threadId, int processId, NoiseGenerationRequest noiseGenerationRequest) {
            this.threadId = threadId;
            this.processId = processId;
            this.noiseGenerationRequest = noiseGenerationRequest;
        }

        public long getThreadId() {
            return threadId;
        }

        public int getProcessId() {
            return processId;
        }

        public NoiseGenerationRequest getNoiseGenerationRequest() {
            return noiseGenerationRequest;
        }
    }

    /**
     * The `GPUMemoryController` class manages the memory pointers on the GPU. These pointers are used to reduce the overhead of allocating new space on the GPU.
     *  * Each memory allocation handled by this class is for a 32 block height section of a region (512 by 512 by 32 blocks). This corresponds to roughly 41MB of Video RAM (VRAM).
     */
    private class GPUMemoryController {

        private final ArrayList<GPUMemoryBlock> gpuMemoryBlockArrayList;

        /**
         *
         */
        private final ArrayList<Boolean> isMemoryAvailableArray;
        /**
         * Maps from the key generated in getMemoryMapKey to the unique allocation for that process.
         */
        private final HashMap<String, Integer> processToMemoryIndexMap;


        public GPUMemoryController(){
            isMemoryAvailableArray=new ArrayList<>(GPU_MEMORY_ALLOCATIONS);
            processToMemoryIndexMap =new HashMap<>();

            gpuMemoryBlockArrayList = new ArrayList<>();

            for (int i = 0; i< GPU_MEMORY_ALLOCATIONS; i++){
                isMemoryAvailableArray.add(true);
                gpuMemoryBlockArrayList.add(new GPUMemoryBlock());
            }
        }

        /**
         * getMemoryMapKey gets the key used to allocate and free a memory allocation.
         * @param threadId The id of thread from Thread.currentThread().getId().
         * @param processId Unique incremented int for each allocation on the same thread.
         * @return The key for getting the memory allocation in the form "threadId-processId"
         */
        private String getMemoryMapKey(long threadId, int processId){
            return threadId+"-"+processId;
        }

        /**
         * Retrieves the memory index for the given process.
         * The process must have already reserved a spot using reserveGPUMemorySpace.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The memory pointer from the GPU pointer array at the calculated index.
         */
        private int getMemoryIndex(long threadId, int processId){
            String mapKey=getMemoryMapKey(threadId,processId);
            return this.processToMemoryIndexMap.get(mapKey);
        }

        //region Getters/Setters

        /**
         * Retrieves the X GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The X GPU memory pointer.
         */
        public long getXGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        /**
         * Retrieves the Y GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The Y GPU memory pointer.
         */
        public long getYGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        /**
         * Retrieves the Z GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The Z GPU memory pointer.
         */
        public long getZGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        /**
         * Retrieves the P GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The P GPU memory pointer.
         */
        public long getPGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        /**
         * Retrieves the output GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The output GPU memory pointer.
         */
        public long getOutputGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        /**
         * Retrieves the compacted output GPU memory pointer for a specific thread and process.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return The compacted output GPU memory pointer.
         */
        public long getCompactedOutputGPUMemoryPointer(long threadId, int processId){
            int index= getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(index).getxGPUPointer();
        }

        //endregion

        //region GPU Memory Reservation

        /**
         * Reserves a GPU memory space for a specific thread and process. If a memory space is available, it is marked as reserved and mapped to the thread and process.
         * This method is thread-safe and can be called concurrently from multiple threads.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId A unique integer incremented for each allocation on the same thread.
         * @return true if a memory space was successfully reserved; false otherwise.
         */
        public synchronized boolean reserveGPUMemorySpace(long threadId, int processId){
            for (int memoryIndex=0; memoryIndex<this.isMemoryAvailableArray.size(); memoryIndex++) {
                if (isMemoryAvailableArray.get(memoryIndex)) {
                    //reserve
                    isMemoryAvailableArray.set(memoryIndex,false);
                    String uniqueProcessMapKey=getMemoryMapKey(threadId,processId);
                    processToMemoryIndexMap.put(uniqueProcessMapKey,memoryIndex);
                    return true;
                }
            }
            return false;
        }

        /**
         * Unreserves a GPU memory space for a specific thread and process. If the thread and process have a reserved memory space, it is unreserved and the mapping is removed.
         * This method is thread-safe and can be called concurrently from multiple threads.
         * @param threadId The ID of the thread, typically obtained from Thread.currentThread().getId().
         * @param processId The unique integer associated with each allocation on the same thread.
         * @return true if a memory space was successfully unreserved; false otherwise.
         */
        public synchronized boolean unreserveGPUMemorySpace(long threadId, int processId){
            String uniqueProcessMapKey=getMemoryMapKey(threadId,processId);

            if (!processToMemoryIndexMap.containsKey(uniqueProcessMapKey)){
                return false;
            }

            int memoryIndex=getMemoryIndex(threadId,processId);

            isMemoryAvailableArray.set(memoryIndex,true);

            processToMemoryIndexMap.remove(uniqueProcessMapKey);
            return true;
        }

        //endregion

        private boolean doesProcessHaveReservedMemory(long threadId, int processId){
            String memoryIndex=getMemoryMapKey(threadId,processId);
            return this.processToMemoryIndexMap.containsKey(memoryIndex);
        }

        public GPUMemoryBlock getGPUMemoryBlock(long threadId, int processId){
            int memoryIndex = getMemoryIndex(threadId,processId);
            return gpuMemoryBlockArrayList.get(memoryIndex);
        }

        public void setGPUMemoryPointersFromResponse(long threadId, int processId, NoiseHardwareAcceleratorResponse response){
            String uniqueProcessMapKey = getMemoryMapKey(threadId,processId);
            int memoryIndex= this.processToMemoryIndexMap.get(uniqueProcessMapKey);

            GPUMemoryBlock responseGPUMemoryBlock = response.getGpuMemoryBlock();

            gpuMemoryBlockArrayList.set(memoryIndex,responseGPUMemoryBlock);
        }
    }

    public void addGPURequestToQueue(long threadId, int processId, NoiseGenerationRequest noiseGenerationRequest){
        NoiseRequestQueueEntry noiseRequestQueueEntry = new NoiseRequestQueueEntry(threadId,processId,noiseGenerationRequest);
        noiseRequestQueue.add(noiseRequestQueueEntry);
    }

    public void startGPUThreadPool(){

        for (int i=0; i<GPU_MEMORY_ALLOCATIONS; i++){
            gpuExecutorService.execute(() ->{
                while (!gpuExecutorService.isShutdown()){
                    GPUNoiseRequest gpuNoiseRequest = tryGetFirstGPUNoiseRequest();
                    if (gpuNoiseRequest==null){
                        try {
                            Thread.sleep(1000);
                            continue;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    long threadId = gpuNoiseRequest.getThreadId();
                    int processId = gpuNoiseRequest.getProcessId();
                    gpuMemoryController.setGPUMemoryPointersFromResponse(threadId, processId, gpuNoiseRequest.execute());
                    gpuMemoryController.unreserveGPUMemorySpace(threadId,processId);
                }
            });
        }

    }

    private synchronized GPUNoiseRequest tryGetFirstGPUNoiseRequest(){
        NoiseRequestQueueEntry noiseRequestQueueEntry = noiseRequestQueue.peek();
        if (noiseRequestQueueEntry==null) {
            return null;
        }

        long threadId = noiseRequestQueueEntry.getThreadId();
        int processId = noiseRequestQueueEntry.getProcessId();
        if (!gpuMemoryController.reserveGPUMemorySpace(threadId,processId)){
            return null;
        }
        noiseRequestQueue.remove();

        NoiseGenerationRequest noiseGenerationRequest = noiseRequestQueueEntry.getNoiseGenerationRequest();
        GPUMemoryBlock gpuMemoryBlock = gpuMemoryController.getGPUMemoryBlock(threadId, processId);

        return new GPUNoiseRequest(noiseGenerationRequest, gpuMemoryBlock, threadId, processId);
    }

    public static NoiseHardwareAccelerator getInstance(){
        if (instance==null) {
            instance = new NoiseHardwareAccelerator();
        }
        return instance;
    }

    /**
     * Whether we are using GPU acceleration. todo make this a setting accessible in the UI
     */
    public final static boolean isGPUEnabled =true;

    /**
     * `GPU_MEMORY_ALLOCATIONS` is a constant that represents the number of memory allocations on the GPU.
     * Each allocation takes around 41 MB of GPU memory. By reusing these allocations, we can significantly reduce the overhead of memory allocation on the GPU.
     * The value of this constant directly impacts the memory usage of our application on the GPU.
     * Please note that increasing this value will increase the VRAM usage of our application.
     * /todo make this a setting in the ui. it would likely be a variable for how much vram to use and would have an automatic option similiar to cpu threads.
     * todo probably good to determine how many allocations a cpu thread needs instead of maxing the memory.
     */
    private final  static int GPU_MEMORY_ALLOCATIONS =10;
}
