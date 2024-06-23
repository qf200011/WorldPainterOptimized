/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.pepsoft.worldpainter.layers.exporters;

import com.google.common.collect.ImmutableMap;
import org.pepsoft.minecraft.Chunk;
import org.pepsoft.minecraft.Material;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.util.Version;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.HeightTransform;
import org.pepsoft.worldpainter.Platform;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.exporting.AbstractLayerExporter;
import org.pepsoft.worldpainter.exporting.FirstPassLayerExporter;
import org.pepsoft.worldpainter.exporting.NoiseHardwareAccelerator;
import org.pepsoft.worldpainter.exporting.gpuacceleration.GPUOptimizable;
import org.pepsoft.worldpainter.exporting.gpuacceleration.NoiseGenerationRequest;
import org.pepsoft.worldpainter.exporting.gpuacceleration.ResourceNoiseGenerationRequest;
import org.pepsoft.worldpainter.layers.Resources;
import org.pepsoft.worldpainter.layers.Void;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static org.pepsoft.minecraft.Constants.*;
import static org.pepsoft.minecraft.Material.*;
import static org.pepsoft.util.MathUtils.clamp;
import static org.pepsoft.worldpainter.Constants.*;
import static org.pepsoft.worldpainter.DefaultPlugin.ATTRIBUTE_MC_VERSION;
import static org.pepsoft.worldpainter.Dimension.Role.CAVE_FLOOR;
import static org.pepsoft.worldpainter.layers.exporters.ResourcesExporter.ResourcesExporterSettings.defaultSettings;

/**
 *
 * @author pepijn
 */
public class ResourcesExporter extends AbstractLayerExporter<Resources> implements FirstPassLayerExporter, GPUOptimizable {
    public ResourcesExporter(Dimension dimension, Platform platform, ExporterSettings settings) {
        super(dimension, platform, (settings != null) ? settings : defaultSettings(platform, dimension.getAnchor(), dimension.getMinHeight(), dimension.getMaxHeight()), Resources.INSTANCE);
        final ResourcesExporterSettings resourcesSettings = (ResourcesExporterSettings) super.settings;
        final Set<Material> allMaterials = resourcesSettings.getMaterials();
        final List<Material> activeMaterials = new ArrayList<>(allMaterials.size());
        for (Material material: allMaterials) {
            if (resourcesSettings.getChance(material) > 0) {
                activeMaterials.add(material);
            }
        }
        this.threadAndProcessToNoiseIndexesMap = new HashMap<>();
        this.activeMaterials = activeMaterials.toArray(new Material[activeMaterials.size()]);
        noiseGenerators = new PerlinNoise[this.activeMaterials.length];
        final long[] seedOffsets = new long[this.activeMaterials.length];
        minLevels = new int[this.activeMaterials.length];
        maxLevels = new int[this.activeMaterials.length];
        chances = new float[this.activeMaterials.length][16];
        for (int i = 0; i < this.activeMaterials.length; i++) {
            noiseGenerators[i] = new PerlinNoise(0);
            seedOffsets[i] = resourcesSettings.getSeedOffset(this.activeMaterials[i]);
            minLevels[i] = resourcesSettings.getMinLevel(this.activeMaterials[i]);
            maxLevels[i] = resourcesSettings.getMaxLevel(this.activeMaterials[i]);
            chances[i] = new float[16];
            for (int j = 0; j < 16; j++) {
                chances[i][j] = PerlinNoise.getLevelForPromillage(Math.min(resourcesSettings.getChance(this.activeMaterials[i]) * j / 8f, 1000f));
            }
        }
        for (int i = 0; i < this.activeMaterials.length; i++) {
            if (noiseGenerators[i].getSeed() != (dimension.getSeed() + seedOffsets[i])) {
                noiseGenerators[i].setSeed(dimension.getSeed() + seedOffsets[i]);
            }
        }
    }

    @Override
    public void render(Tile tile, Chunk chunk, Point regionCoords) {
        NoiseHardwareAccelerator noiseHardwareAccelerator = NoiseHardwareAccelerator.getInstance();

        final boolean coverSteepTerrain = dimension.isCoverSteepTerrain(), nether = (dimension.getAnchor().dim == DIM_NETHER);

        boolean hasGPUNoise=false;
        if (NoiseHardwareAccelerator.isGPUEnabled&&regionCoords!=null&&threadAndProcessToNoiseIndexesMap.containsKey(Thread.currentThread().getId())){
             render((HashMap<Integer, int[]>) threadAndProcessToNoiseIndexesMap.get(Thread.currentThread().getId()),chunk,nether);
             return;
        }


        final int minimumLevel = ((ResourcesExporterSettings) super.settings).getMinimumLevel();
        final int xOffset = (chunk.getxPos() & 7) << 4;
        final int zOffset = (chunk.getzPos() & 7) << 4;





//        int[] counts = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int localX = xOffset + x, localY = zOffset + z;
                final int worldX = tile.getX() * TILE_SIZE + localX, worldY = tile.getY() * TILE_SIZE + localY;
                if (tile.getBitLayerValue(Void.INSTANCE, localX, localY)) {
                    continue;
                }
                final int resourcesValue = Math.max(minimumLevel, tile.getLayerValue(Resources.INSTANCE, localX, localY));
                if (resourcesValue > 0) {
                    final int terrainheight = tile.getIntHeight(localX, localY);
                    final int topLayerDepth = dimension.getTopLayerDepth(worldX, worldY, terrainheight);
                    int subsurfaceMaxHeight = terrainheight - topLayerDepth;
                    if (coverSteepTerrain) {
                        subsurfaceMaxHeight = Math.min(subsurfaceMaxHeight,
                                Math.min(Math.min(dimension.getIntHeightAt(worldX - 1, worldY, Integer.MAX_VALUE),
                                        dimension.getIntHeightAt(worldX + 1, worldY, Integer.MAX_VALUE)),
                                        Math.min(dimension.getIntHeightAt(worldX, worldY - 1, Integer.MAX_VALUE),
                                                dimension.getIntHeightAt(worldX, worldY + 1, Integer.MAX_VALUE))));
                    }
                    final double dx = worldX / TINY_BLOBS, dy = worldY / TINY_BLOBS;
                    final double dirtX = worldX / SMALL_BLOBS, dirtY = worldY / SMALL_BLOBS;
                    // Capping to maxY really shouldn't be necessary, but we've
                    // had several reports from the wild of this going higher
                    // than maxHeight, so there must be some obscure way in
                    // which the terrainHeight can be raised too high
                    for (int y = Math.min(subsurfaceMaxHeight, maxZ); y > minZ; y--) {
                        final double dz = y / TINY_BLOBS;
                        final double dirtZ = y / SMALL_BLOBS;
                        for (int i = 0; i < activeMaterials.length; i++) {
                            final float chance = chances[i][resourcesValue];

                            if ((chance <= 0.5f)
                                && (y >= minLevels[i])
                                && (y <= maxLevels[i]))
                            {

                             if (activeMaterials[i].isNamedOneOf(MC_DIRT, MC_GRAVEL))
                             {
                                 if (noiseGenerators[i].getPerlinNoise(dirtX, dirtY, dirtZ) < chance) continue;
                             }
                             else {
                                 double noise;
                                 if (!NoiseHardwareAccelerator.isGPUEnabled){
                                     noise = noiseGenerators[i].getPerlinNoise(dx, dy, dz);
                                 }

                                 else{
                                             noise = noiseGenerators[i].getPerlinNoise(dx, dy, dz);

                                 }
                                 if (noise < chance){
                                     continue;
                                 }
                             }




//                                counts[oreType]++;
                                final Material existingMaterial = chunk.getMaterial(x, y, z);
                                if (existingMaterial.isNamed(MC_DEEPSLATE) && ORE_TO_DEEPSLATE_VARIANT.containsKey(activeMaterials[i].name)) {
                                    chunk.setMaterial(x, y, z, ORE_TO_DEEPSLATE_VARIANT.get(activeMaterials[i].name));
                                } else if (nether && (activeMaterials[i].isNamed(MC_GOLD_ORE))) {
                                    chunk.setMaterial(x, y, z, NETHER_GOLD_ORE);
                                } else {
                                    chunk.setMaterial(x, y, z, activeMaterials[i]);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
//        System.out.println("Tile " + tile.getX() + "," + tile.getY());
//        for (i = 0; i < 256; i++) {
//            if (counts[i] > 0) {
//                System.out.printf("Exported %6d of ore type %3d%n", counts[i], i);
//            }
//        }
//        System.out.println();
    }

    private void render(HashMap<Integer,int[]> materialToCoordMap, Chunk chunk, boolean nether){
        for (int i = 0; i < activeMaterials.length; i++) {
            if (!materialToCoordMap.containsKey(i)){
                break;
            }
            for (int index:materialToCoordMap.get(i)){
                int tempIndex=index;
                int y = index/(512*512)-1-minLevels[i];
                tempIndex-=y*(512*512);
                int x=tempIndex%512;
                int z=tempIndex/512;

                int lowerXBound=chunk.getxPos()*16;
                int upperXBound=chunk.getxPos()*16+15;

                int lowerZBound=chunk.getzPos()*16;
                int upperZBound=chunk.getzPos()*16+15;

                if (x<lowerXBound||x>upperXBound){
                    break;
                }

                if (z<lowerZBound|| z>upperZBound){
                    break;
                }

                final Material existingMaterial = chunk.getMaterial(x, y, z);
                if (existingMaterial.isNamed(MC_DEEPSLATE) && ORE_TO_DEEPSLATE_VARIANT.containsKey(activeMaterials[i].name)) {
                    chunk.setMaterial(x, y, z, ORE_TO_DEEPSLATE_VARIANT.get(activeMaterials[i].name));
                } else if (nether && (activeMaterials[i].isNamed(MC_GOLD_ORE))) {
                    chunk.setMaterial(x, y, z, NETHER_GOLD_ORE);
                } else {
                    chunk.setMaterial(x, y, z, activeMaterials[i]);
                }
                break;
            }
        }
    }

//  TODO: resource frequenties onderzoeken met Statistics tool!

    //TODO keep this private and move the region arrays to the exporter classes. Java handles the data prep instead of c
    public final Material[] activeMaterials;
    public final PerlinNoise[] noiseGenerators;
    public final int[] minLevels, maxLevels;
    private final float[][] chances;
    private final Map<Long,Map<Integer,int[]>> threadAndProcessToNoiseIndexesMap;

    private static final Map<String, Material> ORE_TO_DEEPSLATE_VARIANT = ImmutableMap.of(
            MC_COAL_ORE, DEEPSLATE_COAL_ORE,
            MC_COPPER_ORE, DEEPSLATE_COPPER_ORE,
            MC_LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
            MC_IRON_ORE, DEEPSLATE_IRON_ORE,
            MC_GOLD_ORE, DEEPSLATE_GOLD_ORE,
            MC_REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
            MC_DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
            MC_EMERALD_ORE, DEEPSLATE_EMERALD_ORE
    );

    private String getUniqueKeyForNoiseMap(long threadId, int processId){
        return threadId+"-"+processId;
    }

    @Override
    public void computePerlinNoiseOnGPU(Point region) {
        for (int materialIndex=0;materialIndex<activeMaterials.length; materialIndex++){
            final float blobSize;
            if (activeMaterials[materialIndex].isNamedOneOf(MC_DIRT, MC_GRAVEL)){
                blobSize=SMALL_BLOBS;
            } else{
                blobSize=TINY_BLOBS;
            }

            final long seed = noiseGenerators[materialIndex].getSeed();

            int minMaterialHeight = minLevels[materialIndex];
            int maxMaterialHeight = maxLevels[materialIndex];
            int heightSize = NoiseGenerationRequest.HEIGHT_SIZE;

            for (int startingHeight=minMaterialHeight; startingHeight<=maxMaterialHeight; startingHeight=startingHeight+heightSize) { //todo, including max?
                ResourceNoiseGenerationRequest resourceNoiseHardwareAcceleratorRequest = new ResourceNoiseGenerationRequest(seed,region.x,region.y,startingHeight,blobSize,this,chances[materialIndex]);
                long threadId=Thread.currentThread().getId();
                NoiseHardwareAccelerator.getInstance().addGPURequestToQueue(threadId,materialIndex,resourceNoiseHardwareAcceleratorRequest); //use materialIndex since it's unique per thread.
            }
        }
    }

    @Override
    public synchronized void computeCallBack(long threadId, int processId, int[] outputIndexes) {
        if (!threadAndProcessToNoiseIndexesMap.containsKey(threadId)){
            threadAndProcessToNoiseIndexesMap.put(threadId, new HashMap<>());
        }

        threadAndProcessToNoiseIndexesMap.get(threadId).put(processId,outputIndexes);
    }

    public static class ResourcesExporterSettings implements ExporterSettings {
        private ResourcesExporterSettings(Map<Material, ResourceSettings> settings) {
            this.settings = settings;
        }

        @Override
        public boolean isApplyEverywhere() {
            return minimumLevel > 0;
        }

        public int getMinimumLevel() {
            return minimumLevel;
        }

        public void setMinimumLevel(int minimumLevel) {
            this.minimumLevel = minimumLevel;
        }
        
        public Set<Material> getMaterials() {
            return settings.keySet();
        }
        
        public int getMinLevel(Material material) {
            return settings.get(material).minLevel;
        }
        
        public void setMinLevel(Material material, int minLevel) {
            settings.get(material).minLevel = minLevel;
        }
        
        public int getMaxLevel(Material material) {
            return settings.get(material).maxLevel;
        }
        
        public void setMaxLevel(Material material, int maxLevel) {
            settings.get(material).maxLevel = maxLevel;
        }
        
        public int getChance(Material material) {
            return settings.get(material).chance;
        }
        
        public void setChance(Material material, int chance) {
            settings.get(material).chance = chance;
        }

        public long getSeedOffset(Material material) {
            return settings.get(material).seedOffset;
        }
        
        @Override
        public Resources getLayer() {
            return Resources.INSTANCE;
        }

        @Override
        public void setMinMaxHeight(int oldMinHeight, int newMinHeight, int oldMaxHeight, int newMaxHeight, HeightTransform transform) {
            for (Material material: settings.keySet()) {
                int maxLevel = settings.get(material).maxLevel;
                if (maxLevel == (oldMaxHeight - 1)) {
                    maxLevel = newMaxHeight - 1;
                } else if (maxLevel > 1) {
                    maxLevel = clamp(newMinHeight, transform.transformHeight(maxLevel), newMaxHeight - 1);
                }
                // TODO: do the same for minLevels? Or do we WANT those to stay put?
                settings.get(material).maxLevel = maxLevel;
            }
        }

        @Override
        public ResourcesExporterSettings clone() {
            try {
                ResourcesExporterSettings clone = (ResourcesExporterSettings) super.clone();
                clone.settings = new LinkedHashMap<>();
                settings.forEach((material, settings) -> clone.settings.put(material, settings.clone()));
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        public static ResourcesExporterSettings defaultSettings(Platform platform, Dimension.Anchor anchor, int minHeight, int maxHeight) {
            final Random random = new Random();
            final Map<Material, ResourceSettings> settings = new HashMap<>();
            final Version mcVersion = platform.getAttribute(ATTRIBUTE_MC_VERSION);
            switch (anchor.dim) {
                case DIM_NORMAL:
                    // TODO make these normal distributions or something else more similar to Minecraft
                    settings.put(DIRT,             new ResourceSettings(DIRT,             0,         maxHeight - 1, 57,     random.nextLong()));
                    settings.put(GRAVEL,           new ResourceSettings(GRAVEL,           minHeight, maxHeight - 1, 28,     random.nextLong()));
                    settings.put(GOLD_ORE,         new ResourceSettings(GOLD_ORE,         minHeight,            31,  1,     random.nextLong()));
                    settings.put(IRON_ORE,         new ResourceSettings(IRON_ORE,         minHeight, maxHeight - 1,  6,     random.nextLong()));
                    settings.put(COAL,             new ResourceSettings(COAL,             0,         maxHeight - 1, 10,     random.nextLong()));
                    settings.put(LAPIS_LAZULI_ORE, new ResourceSettings(LAPIS_LAZULI_ORE, minHeight,            31,  1,     random.nextLong()));
                    settings.put(DIAMOND_ORE,      new ResourceSettings(DIAMOND_ORE,      minHeight,            15,  1,     random.nextLong()));
                    settings.put(REDSTONE_ORE,     new ResourceSettings(REDSTONE_ORE,     minHeight,            15,  8,     random.nextLong()));
                    settings.put(STATIONARY_WATER, new ResourceSettings(STATIONARY_WATER, minHeight, maxHeight - 1,  1,     random.nextLong()));
                    settings.put(STATIONARY_LAVA,  new ResourceSettings(STATIONARY_LAVA,  minHeight,            15,  2,     random.nextLong()));
                    settings.put(EMERALD_ORE,      new ResourceSettings(EMERALD_ORE,      64,        maxHeight - 1, mcVersion.isAtLeast(V_1_2) ? 1 : 0,
                                                                                                                            random.nextLong()));
                    settings.put(COPPER_ORE,       new ResourceSettings(COPPER_ORE,       0,         88,            mcVersion.isAtLeast(V_1_17) ? 6 : 0,
                                                                                                                            random.nextLong()));

                    settings.put(QUARTZ_ORE,     new ResourceSettings(QUARTZ_ORE,     minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(ANCIENT_DEBRIS, new ResourceSettings(ANCIENT_DEBRIS, minHeight, maxHeight - 1, 0, random.nextLong()));
                    break;
                case DIM_NETHER:
                    settings.put(QUARTZ_ORE,     new ResourceSettings(QUARTZ_ORE,     minHeight, maxHeight - 1, mcVersion.isAtLeast(V_1_2) ? 7 : 0,
                                                                                                                       random.nextLong()));
                    settings.put(GOLD_ORE,       new ResourceSettings(GOLD_ORE,       minHeight, maxHeight - 1, mcVersion.isAtLeast(V_1_15) ? 3 : 0,
                                                                                                                       random.nextLong()));
                    settings.put(ANCIENT_DEBRIS, new ResourceSettings(ANCIENT_DEBRIS, minHeight, maxHeight - 1, mcVersion.isAtLeast(V_1_15) ? 1 : 0,
                                                                                                                       random.nextLong()));

                    settings.put(DIRT,             new ResourceSettings(DIRT,             0,         maxHeight - 1, 0, random.nextLong()));
                    settings.put(GRAVEL,           new ResourceSettings(GRAVEL,           minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(IRON_ORE,         new ResourceSettings(IRON_ORE,         minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(COAL,             new ResourceSettings(COAL,             0,         maxHeight - 1, 0, random.nextLong()));
                    settings.put(LAPIS_LAZULI_ORE, new ResourceSettings(LAPIS_LAZULI_ORE, minHeight,            31, 0, random.nextLong()));
                    settings.put(DIAMOND_ORE,      new ResourceSettings(DIAMOND_ORE,      minHeight,            15, 0, random.nextLong()));
                    settings.put(REDSTONE_ORE,     new ResourceSettings(REDSTONE_ORE,     minHeight,            15, 0, random.nextLong()));
                    settings.put(STATIONARY_WATER, new ResourceSettings(STATIONARY_WATER, minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(STATIONARY_LAVA,  new ResourceSettings(STATIONARY_LAVA,  minHeight,            15, 0, random.nextLong()));
                    settings.put(EMERALD_ORE,      new ResourceSettings(EMERALD_ORE,      64,        maxHeight - 1, 0, random.nextLong()));
                    settings.put(COPPER_ORE,       new ResourceSettings(COPPER_ORE,       0,                    88, 0, random.nextLong()));
                    break;
                case DIM_END:
                    settings.put(DIRT,             new ResourceSettings(DIRT,             0,         maxHeight - 1, 0, random.nextLong()));
                    settings.put(GRAVEL,           new ResourceSettings(GRAVEL,           minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(GOLD_ORE,         new ResourceSettings(GOLD_ORE,         minHeight,            31, 0, random.nextLong()));
                    settings.put(IRON_ORE,         new ResourceSettings(IRON_ORE,         minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(COAL,             new ResourceSettings(COAL,             0,         maxHeight - 1, 0, random.nextLong()));
                    settings.put(LAPIS_LAZULI_ORE, new ResourceSettings(LAPIS_LAZULI_ORE, minHeight,            31, 0, random.nextLong()));
                    settings.put(DIAMOND_ORE,      new ResourceSettings(DIAMOND_ORE,      minHeight,            15, 0, random.nextLong()));
                    settings.put(REDSTONE_ORE,     new ResourceSettings(REDSTONE_ORE,     minHeight,            15, 0, random.nextLong()));
                    settings.put(STATIONARY_WATER, new ResourceSettings(STATIONARY_WATER, minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(STATIONARY_LAVA,  new ResourceSettings(STATIONARY_LAVA,  minHeight,            15, 0, random.nextLong()));
                    settings.put(EMERALD_ORE,      new ResourceSettings(EMERALD_ORE,      64,        maxHeight - 1, 0, random.nextLong()));
                    settings.put(COPPER_ORE,       new ResourceSettings(COPPER_ORE,       0,                    88, 0, random.nextLong()));
                    settings.put(QUARTZ_ORE,       new ResourceSettings(QUARTZ_ORE,       minHeight, maxHeight - 1, 0, random.nextLong()));
                    settings.put(ANCIENT_DEBRIS,   new ResourceSettings(ANCIENT_DEBRIS,   minHeight, maxHeight - 1, 0, random.nextLong()));
                    break;
                default:
                    throw new IllegalArgumentException("Dimension " + anchor.dim + " not supported");
            }
            final ResourcesExporterSettings result = new ResourcesExporterSettings(settings);
            if (anchor.role == CAVE_FLOOR) {
                result.setMinimumLevel(0);
            }
            return result;
        }
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            if (version < 1) {
                // Legacy conversions
                // Fix static water and lava
                if (! maxLevels.containsKey(BLK_WATER)) {
                    logger.warn("Fixing water and lava settings");
                    maxLevels.put(BLK_WATER, maxLevels.get(BLK_STATIONARY_WATER));
                    chances.put(BLK_WATER, chances.get(BLK_STATIONARY_WATER));
                    seedOffsets.put(BLK_WATER, seedOffsets.get(BLK_STATIONARY_WATER));
                    maxLevels.put(BLK_LAVA, maxLevels.get(BLK_STATIONARY_LAVA));
                    chances.put(BLK_LAVA, chances.get(BLK_STATIONARY_LAVA));
                    seedOffsets.put(BLK_LAVA, seedOffsets.get(BLK_STATIONARY_LAVA));
                    maxLevels.remove(BLK_STATIONARY_WATER);
                    chances.remove(BLK_STATIONARY_WATER);
                    seedOffsets.remove(BLK_STATIONARY_WATER);
                    maxLevels.remove(BLK_STATIONARY_LAVA);
                    chances.remove(BLK_STATIONARY_LAVA);
                    seedOffsets.remove(BLK_STATIONARY_LAVA);
                }
                if (! maxLevels.containsKey(BLK_EMERALD_ORE)) {
                    maxLevels.put(BLK_EMERALD_ORE, 31);
                    chances.put(BLK_EMERALD_ORE, 0);
                }
                Random random = new Random();
                if (! seedOffsets.containsKey(BLK_EMERALD_ORE)) {
                    seedOffsets.put(BLK_EMERALD_ORE, random.nextLong());
                }
                if (minLevels == null) {
                    minLevels = new HashMap<>();
                    for (int blockType: maxLevels.keySet()) {
                        minLevels.put(blockType, 0);
                    }
                }
                if (! minLevels.containsKey(BLK_QUARTZ_ORE)) {
                    minLevels.put(BLK_QUARTZ_ORE, 0);
                    maxLevels.put(BLK_QUARTZ_ORE, 255);
                    chances.put(BLK_QUARTZ_ORE, 0);
                    seedOffsets.put(BLK_QUARTZ_ORE, random.nextLong());
                }

                // Convert integer-based settings to material-based settings
                settings = new LinkedHashMap<>();
                for (int blockType: maxLevels.keySet()) {
                    Material material = get(blockType);
                    settings.put(material, new ResourceSettings(material, minLevels.get(blockType), maxLevels.get(blockType),
                            chances.get(blockType), seedOffsets.get(blockType)));
                }
                minLevels = null;
                maxLevels = null;
                chances = null;
                seedOffsets = null;
            }
            if (version < 2) {
                // Add new resources
                final Random random = new Random();
                settings.put(COPPER_ORE,     new ResourceSettings(COPPER_ORE,       0,  88, 0, random.nextLong()));
                settings.put(ANCIENT_DEBRIS, new ResourceSettings(ANCIENT_DEBRIS, -64, 319, 0, random.nextLong()));
            }
            if (version < 3) {
                if (settings.containsKey(WATER)) {
                    settings.put(STATIONARY_WATER, settings.get(WATER));
                    settings.remove(WATER);
                }
                if (settings.containsKey(LAVA)) {
                    settings.put(STATIONARY_LAVA, settings.get(LAVA));
                    settings.remove(LAVA);
                }
            }
            version = 3;
        }
        
        private int minimumLevel = 8;
        private Map<Material, ResourceSettings> settings;
        /** @deprecated */
        @Deprecated private Map<Integer, Integer> maxLevels = null;
        /** @deprecated */
        @Deprecated private Map<Integer, Integer> chances = null;
        /** @deprecated */
        @Deprecated private Map<Integer, Long> seedOffsets = null;
        /** @deprecated */
        @Deprecated private Map<Integer, Integer> minLevels = null;
        private int version = 3;

        private static final long serialVersionUID = 1L;
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourcesExporter.class);
    }

    static class ResourceSettings implements Serializable, Cloneable {
        ResourceSettings(Material material, int minLevel, int maxLevel, int chance, long seedOffset) {
            this.material = material;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.chance = chance;
            this.seedOffset = seedOffset;
        }

        @Override
        public ResourceSettings clone() {
            try {
                return (ResourceSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        Material material;
        int minLevel, maxLevel, chance;
        long seedOffset;

        private static final long serialVersionUID = 1L;
    }
}