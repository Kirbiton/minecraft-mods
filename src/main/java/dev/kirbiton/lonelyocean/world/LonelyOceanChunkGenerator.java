package dev.kirbiton.lonelyocean.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * A deterministic, ocean-only overworld terrain generator.
 * The selected biome source and vanilla structure pipeline are retained.
 */
public final class LonelyOceanChunkGenerator extends ChunkGenerator {
    public static final int MIN_Y = -64;
    public static final int WORLD_HEIGHT = 384;
    public static final int SEA_LEVEL = IslandShape.SEA_LEVEL;
    private static final int OCEAN_FLOOR_BASE = 32;

    public static final MapCodec<LonelyOceanChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
                    .apply(instance, LonelyOceanChunkGenerator::new));

    public LonelyOceanChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    public static void register() {
        net.minecraft.core.Registry.register(
                BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath("lonely_ocean", "ocean_island"),
                CODEC);
    }

    /** Keep vanilla structure sets active and use the island chunk as the dimension origin. */
    @Override
    public ChunkGeneratorStructureState createState(
            HolderLookup<StructureSet> structureSets,
            RandomState randomState,
            long legacyLevelSeed) {
        return ChunkGeneratorStructureState.createForNormal(
                randomState, legacyLevelSeed, getOrigin(randomState), getBiomeSource(), structureSets);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(
            ChunkAccess chunk,
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            BiomeManager biomeManager,
            WorldGenRegion carverBiomeRegion,
            Set<Holder<Biome>> possibleBiomes) {
        long seed = randomState.seed();
        IslandShape island = IslandShape.fromSeed(seed);
        ChunkPos chunkPos = chunk.getPos();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                int islandSurface = island.surfaceY(worldX, worldZ);
                boolean land = islandSurface != Integer.MIN_VALUE;
                int floorY = land ? islandSurface : oceanFloorY(worldX, worldZ, seed);

                for (int y = minY; y <= floorY && y < maxY; y++) {
                    BlockState state = terrainBlock(worldX, y, worldZ, floorY, land, island, seed);
                    chunk.setBlockState(new BlockPos(worldX, y, worldZ), state, 0);
                }

                if (!land) {
                    for (int y = floorY + 1; y <= SEA_LEVEL && y < maxY; y++) {
                        if (y >= minY) {
                            chunk.setBlockState(new BlockPos(worldX, y, worldZ), Blocks.WATER.defaultBlockState(), 0);
                        }
                    }
                }
            }
        }

        placeStarterTrees(chunk, island, seed, minY, maxY);
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState terrainBlock(int x, int y, int z, int floorY, boolean land, IslandShape island, long seed) {
        if (y == MIN_Y) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        if (!land && y < floorY - 4 && y > MIN_Y + 5
                && UnderwaterCaves.isCave(x, y, z, seed)) {
            return Blocks.WATER.defaultBlockState();
        }
        if (y < floorY - 3) {
            return Blocks.STONE.defaultBlockState();
        }
        if (land) {
            if (island.isBeach(x, z)) {
                return Blocks.SAND.defaultBlockState();
            }
            return y == floorY ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        return ((coordinateHash(x, z, seed) & 3L) == 0L)
                ? Blocks.GRAVEL.defaultBlockState()
                : Blocks.SAND.defaultBlockState();
    }

    private static int oceanFloorY(int x, int z, long seed) {
        return OCEAN_FLOOR_BASE + (int) Math.floorMod(coordinateHash(x, z, seed), 7L);
    }

    private static long coordinateHash(int x, int z, long seed) {
        long value = seed ^ ((long) x * 0x9E3779B185EBCA87L) ^ ((long) z * 0xC2B2AE3D27D4EB4FL);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static void placeStarterTrees(ChunkAccess chunk, IslandShape island, long seed, int minY, int maxY) {
        java.util.SplittableRandom random = new java.util.SplittableRandom(seed ^ 0x4C4F4E454C594F43L);
        int treeCount = 2 + random.nextInt(2);
        ChunkPos pos = chunk.getPos();
        for (int tree = 0; tree < treeCount; tree++) {
            int treeX = island.centerX() + random.nextInt(-5, 6);
            int treeZ = island.centerZ() + random.nextInt(-5, 6);
            if (!island.contains(treeX, treeZ) || island.isBeach(treeX, treeZ)) {
                treeX = island.centerX();
                treeZ = island.centerZ();
            }
            if (treeX < pos.getMinBlockX() || treeX > pos.getMaxBlockX()
                    || treeZ < pos.getMinBlockZ() || treeZ > pos.getMaxBlockZ()) {
                continue;
            }
            int groundY = island.surfaceY(treeX, treeZ);
            if (groundY == Integer.MIN_VALUE || groundY + 7 >= maxY || groundY < minY) {
                continue;
            }
            boolean birch = random.nextBoolean();
            BlockState log = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState();
            BlockState leaves = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState();
            for (int dy = 1; dy <= 4; dy++) {
                chunk.setBlockState(new BlockPos(treeX, groundY + dy, treeZ), log, 0);
            }
            for (int dy = 3; dy <= 5; dy++) {
                int radius = dy == 5 ? 1 : 2;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > radius * 2 - 1) {
                            continue;
                        }
                        int leafX = treeX + dx;
                        int leafZ = treeZ + dz;
                        if (leafX >= pos.getMinBlockX() && leafX <= pos.getMaxBlockX()
                                && leafZ >= pos.getMinBlockZ() && leafZ <= pos.getMaxBlockZ()) {
                            BlockPos leafPos = new BlockPos(leafX, groundY + dy, leafZ);
                            if (chunk.getBlockState(leafPos).isAir()) {
                                chunk.setBlockState(leafPos, leaves, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        int surface = island.surfaceY(x, z);
        int floor = surface == Integer.MIN_VALUE ? oceanFloorY(x, z, randomState.seed()) : surface;
        if (type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG) {
            return floor + 1;
        }
        return surface == Integer.MIN_VALUE ? SEA_LEVEL + 1 : surface + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        int minY = heightAccessor.getMinY();
        int height = heightAccessor.getHeight();
        BlockState[] states = new BlockState[height];
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        int surface = island.surfaceY(x, z);
        boolean land = surface != Integer.MIN_VALUE;
        int floor = land ? surface : oceanFloorY(x, z, randomState.seed());
        for (int index = 0; index < height; index++) {
            int y = minY + index;
            if (y <= floor) {
                states[index] = terrainBlock(x, y, z, floor, land, island, randomState.seed());
            } else if (!land && y <= SEA_LEVEL) {
                states[index] = Blocks.WATER.defaultBlockState();
            } else {
                states[index] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getGenDepth() {
        return WORLD_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public ChunkPos getOrigin(RandomState randomState) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        return new ChunkPos(island.centerX() >> 4, island.centerZ() >> 4);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return SEA_LEVEL + 1;
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        // Natural mob spawning is handled by the normal biome spawn pipeline.
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        result.add("Lonely Ocean island: " + island.centerX() + ", " + island.centerZ());
    }
}