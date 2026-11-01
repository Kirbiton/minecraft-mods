package dev.kirbiton.lonelyocean.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** A deterministic ocean-only overworld generator with a floating starter island. */
public final class LonelyOceanChunkGenerator extends ChunkGenerator {
    public static final int MIN_Y = -64;
    public static final int WORLD_HEIGHT = 384;
    public static final int SEA_LEVEL = IslandShape.SEA_LEVEL;
    private static final int OCEAN_FLOOR_BASE = 44;

    public static final MapCodec<LonelyOceanChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource))
                    .apply(instance, LonelyOceanChunkGenerator::new));

    public LonelyOceanChunkGenerator(BiomeSource biomeSource) { super(biomeSource); }

    public static void register() {
        net.minecraft.core.Registry.register(BuiltInRegistries.CHUNK_GENERATOR,
                Identifier.fromNamespaceAndPath("lonely_ocean", "ocean_island"), CODEC);
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets,
            RandomState randomState, long legacyLevelSeed) {
        return ChunkGeneratorStructureState.createForNormal(
                randomState, legacyLevelSeed, getOrigin(randomState), getBiomeSource(), structureSets);
    }

    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState,
            StructureManager structureManager, BiomeManager biomeManager, WorldGenRegion carverBiomeRegion,
            Set<Holder<Biome>> possibleBiomes) {
        long seed = randomState.seed();
        IslandShape island = IslandShape.fromSeed(seed);
        ChunkPos cp = chunk.getPos();
        int minY = chunk.getMinY();
        int maxY = minY + chunk.getHeight();

        for (int lx = 0; lx < 16; lx++) {
            int x = cp.getMinBlockX() + lx;
            for (int lz = 0; lz < 16; lz++) {
                int z = cp.getMinBlockZ() + lz;
                int surface = island.surfaceY(x, z);
                boolean land = surface != Integer.MIN_VALUE;
                int floor = land ? surface : oceanFloorY(x, z, seed);
                int bottom = land ? island.bottomY(x, z) : minY;

                for (int y = minY; y <= floor && y < maxY; y++) {
                    BlockState state = terrainBlock(x, y, z, floor, bottom, land, island, seed);
                    chunk.setBlockState(new BlockPos(x, y, z), state, 0);
                }
                if (!land) {
                    for (int y = floor + 1; y <= SEA_LEVEL && y < maxY; y++) {
                        if (y >= minY) chunk.setBlockState(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), 0);
                    }
                }
            }
        }
        placeStarterTrees(chunk, island, seed, minY, maxY);
        return CompletableFuture.completedFuture(chunk);
    }

    private static BlockState terrainBlock(int x, int y, int z, int floorY, int bottomY,
            boolean land, IslandShape island, long seed) {
        if (y <= MIN_Y + 4) { int thickness = 1 + (int) Math.floorMod(coordinateHash(x, z, seed), 5L); if (y <= MIN_Y + thickness - 1) return Blocks.BEDROCK.defaultBlockState(); }
        if (land && y < bottomY) {
            int seabed = oceanFloorY(x, z, seed);
            if (y > seabed) return Blocks.WATER.defaultBlockState();
            return terrainBlock(x, y, z, seabed, MIN_Y, false, island, seed);
        }

        if (!land && y < floorY - 4 && y > MIN_Y + 5 && UnderwaterCaves.isCave(x, y, z, seed)) {
            return Blocks.AIR.defaultBlockState();
        }

        // Match the Overworld's stone -> deepslate transition around Y=0.
        if (y <= -8) return Blocks.DEEPSLATE.defaultBlockState();
        if (y <= 0) {
            long hash = coordinateHash(x, y, z, seed);
            return Math.floorMod(hash, 8L) < (0 - y)
                    ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (y < floorY - 3) return Blocks.STONE.defaultBlockState();

        if (land) {
            if (island.isBeach(x, z)) return Blocks.SAND.defaultBlockState();
            return y == floorY ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        return (coordinateHash(x, z, seed) & 3L) == 0L
                ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
    }

    /** Smooth low-amplitude seabed: broad noise, no independent per-block height lottery. */
    private static int oceanFloorY(int x, int z, long seed) {
        double n = noise2d(x, z, seed, 52.0);
        return OCEAN_FLOOR_BASE + (int) Math.round((n - 0.5) * 4.0);
    }

    private static double noise2d(int x, int z, long seed, double scale) {
        double px = x / scale, pz = z / scale;
        int x0 = (int) Math.floor(px), z0 = (int) Math.floor(pz);
        double tx = smooth(px - x0), tz = smooth(pz - z0);
        double a = lerp(hash01(x0, z0, seed), hash01(x0 + 1, z0, seed), tx);
        double b = lerp(hash01(x0, z0 + 1, seed), hash01(x0 + 1, z0 + 1, seed), tx);
        return lerp(a, b, tz);
    }

    private static double hash01(int x, int z, long seed) {
        long h = coordinateHash(x, z, seed);
        return (h >>> 11) * 0x1.0p-53;
    }
    private static double smooth(double t) { return t * t * (3.0 - 2.0 * t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static long coordinateHash(int x, int z, long seed) {
        long value = seed ^ ((long) x * 0x9E3779B185EBCA87L) ^ ((long) z * 0xC2B2AE3D27D4EB4FL);
        value ^= value >>> 30; value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27; value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
    private static long coordinateHash(int x, int y, int z, long seed) {
        return coordinateHash(x ^ (y * 0x632BE5AB), z + y * 0x85157AF5, seed);
    }

    private static void placeStarterTrees(ChunkAccess chunk, IslandShape island, long seed, int minY, int maxY) {
        java.util.SplittableRandom random = new java.util.SplittableRandom(seed ^ 0x4C4F4E454C594F43L);
        int treeCount = 2 + random.nextInt(2);
        ChunkPos pos = chunk.getPos();
        for (int tree = 0; tree < treeCount; tree++) {
            int treeX = island.centerX() + random.nextInt(-5, 6);
            int treeZ = island.centerZ() + random.nextInt(-5, 6);
            if (!island.contains(treeX, treeZ) || island.isBeach(treeX, treeZ)) {
                treeX = island.centerX(); treeZ = island.centerZ();
            }
            if (treeX < pos.getMinBlockX() || treeX > pos.getMaxBlockX()
                    || treeZ < pos.getMinBlockZ() || treeZ > pos.getMaxBlockZ()) continue;
            int groundY = island.surfaceY(treeX, treeZ);
            if (groundY == Integer.MIN_VALUE || groundY + 7 >= maxY || groundY < minY) continue;
            boolean birch = random.nextBoolean();
            BlockState log = (birch ? Blocks.BIRCH_LOG : Blocks.OAK_LOG).defaultBlockState();
            BlockState leaves = (birch ? Blocks.BIRCH_LEAVES : Blocks.OAK_LEAVES).defaultBlockState();
            for (int dy = 1; dy <= 4; dy++) chunk.setBlockState(new BlockPos(treeX, groundY + dy, treeZ), log, 0);
            for (int dy = 3; dy <= 5; dy++) {
                int radius = dy == 5 ? 1 : 2;
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > radius * 2 - 1) continue;
                    int lx = treeX + dx, lz = treeZ + dz;
                    if (lx >= pos.getMinBlockX() && lx <= pos.getMaxBlockX()
                            && lz >= pos.getMinBlockZ() && lz <= pos.getMaxBlockZ()) {
                        BlockPos p = new BlockPos(lx, groundY + dy, lz);
                        if (chunk.getBlockState(p).isAir()) chunk.setBlockState(p, leaves, 0);
                    }
                }
            }
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor accessor, RandomState randomState) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        int surface = island.surfaceY(x, z);
        int floor = surface == Integer.MIN_VALUE ? oceanFloorY(x, z, randomState.seed()) : surface;
        if (type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG) return floor + 1;
        return surface == Integer.MIN_VALUE ? SEA_LEVEL + 1 : surface + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor, RandomState randomState) {
        int minY = accessor.getMinY(), height = accessor.getHeight();
        BlockState[] states = new BlockState[height];
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        int surface = island.surfaceY(x, z);
        boolean land = surface != Integer.MIN_VALUE;
        int floor = land ? surface : oceanFloorY(x, z, randomState.seed());
        int bottom = land ? island.bottomY(x, z) : minY;
        for (int i = 0; i < height; i++) {
            int y = minY + i;
            if (y <= floor) states[i] = terrainBlock(x, y, z, floor, bottom, land, island, randomState.seed());
            else if (!land && y <= SEA_LEVEL) states[i] = Blocks.WATER.defaultBlockState();
            else states[i] = Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(minY, states);
    }

    @Override public int getMinY() { return MIN_Y; }
    @Override public int getGenDepth() { return WORLD_HEIGHT; }
    @Override public int getSeaLevel() { return SEA_LEVEL; }
    @Override public ChunkPos getOrigin(RandomState randomState) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        return new ChunkPos(island.centerX() >> 4, island.centerZ() >> 4);
    }
    @Override public int getSpawnHeight(LevelHeightAccessor accessor) { return SEA_LEVEL + 1; }

    /**
     * The vanilla ancient-city structure uses beard_box terrain adaptation.
     * NoiseBasedChunkGenerator applies that through the beardifier density
     * function; this hand-built terrain generator has no density pipeline, so
     * reproduce the important result after the city pieces have been placed:
     * hollow the gaps between pieces and a short irregular shell around them.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        if (!structureManager.shouldGenerateStructures()) return;

        Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Identifier ancientCityId = Identifier.fromNamespaceAndPath("minecraft", "ancient_city");
        ChunkPos cp = chunk.getPos();

        List<StructureStart> starts = structureManager.startsForStructure(cp.x(), cp.z(),
                structure -> structures.getResourceKey(structure)
                        .map(key -> key.identifier().equals(ancientCityId))
                        .orElse(false));

        for (StructureStart start : starts) {
            if (!start.isValid()) continue;

            // Carve a small chamber around each individual city piece. Using
            // the whole city's bounding box creates enormous empty halls
            // between distant buildings and still leaves some pieces embedded.
            for (var piece : start.getPieces()) {
                BoundingBox pieceBox = piece.getBoundingBox();
                int minX = Math.max(cp.getMinBlockX(), pieceBox.minX() - 2);
                int maxX = Math.min(cp.getMaxBlockX(), pieceBox.maxX() + 2);
                int minZ = Math.max(cp.getMinBlockZ(), pieceBox.minZ() - 2);
                int maxZ = Math.min(cp.getMaxBlockZ(), pieceBox.maxZ() + 2);
                int minY = Math.max(MIN_Y + 1, pieceBox.minY() - 2);
                int maxY = Math.min(SEA_LEVEL, pieceBox.maxY() + 2);

                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            int dx = Math.max(Math.max(pieceBox.minX() - x, 0), x - pieceBox.maxX());
                            int dy = Math.max(Math.max(pieceBox.minY() - y, 0), y - pieceBox.maxY());
                            int dz = Math.max(Math.max(pieceBox.minZ() - z, 0), z - pieceBox.maxZ());
                            double distance = (dx * dx + dz * dz) / 4.0 + (dy * dy) / 4.0;
                            if ((dx != 0 || dy != 0 || dz != 0) && distance > 1.0) continue;

                            BlockPos pos = new BlockPos(x, y, z);
                            if (!structureManager.structureHasPieceAt(pos, start)) {
                                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), 0);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override public void spawnOriginalMobs(WorldGenRegion region) { }
    @Override public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos,
            SamplerContext samplerContext) {
        IslandShape island = IslandShape.fromSeed(randomState.seed());
        result.add("Lonely Ocean island: " + island.centerX() + ", " + island.centerZ());
    }
}
