package styy.pplShop.pplshop.client.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.HangingSignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import styy.pplShop.pplshop.client.config.ParserRulesConfig;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.ShopSignEntry;
import styy.pplShop.pplshop.client.model.ShopSignFingerprint;
import styy.pplShop.pplshop.client.model.ShopSignParseResult;
import styy.pplShop.pplshop.client.model.SignContainerRelation;
import styy.pplShop.pplshop.client.model.SignSide;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;
import styy.pplShop.pplshop.client.parser.ShopSignParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LoadedSignScanner {
    private final ShopSignParseCache parseCache = new ShopSignParseCache();
    private final NegativeShopCache negativeCache = new NegativeShopCache();
    private final Map<String, ShopSignDiagnosticReason> recentEvents = new LinkedHashMap<>();
    private RefreshJob activeJob;
    private int lastCollectedChunkCount;
    private int lastCollectedSignCount;
    private int lastProcessedSigns;
    private long lastCollectionPhaseNanos;
    private long lastSignPhaseNanos;
    private long totalCollectionPhaseNanos;
    private long totalSignPhaseNanos;
    private long cacheHitCount;
    private long negativeCacheHitCount;
    private long dirtyReparseCount;
    private long fullRebuildCount;
    private boolean worldDirty;

    public void clearParsedEntryCache() {
        this.parseCache.clear();
        this.negativeCache.clear();
        this.recentEvents.clear();
        this.activeJob = null;
        this.lastCollectedChunkCount = 0;
        this.lastCollectedSignCount = 0;
        this.lastProcessedSigns = 0;
        this.lastCollectionPhaseNanos = 0L;
        this.lastSignPhaseNanos = 0L;
        this.totalCollectionPhaseNanos = 0L;
        this.totalSignPhaseNanos = 0L;
        this.cacheHitCount = 0L;
        this.negativeCacheHitCount = 0L;
        this.dirtyReparseCount = 0L;
        this.fullRebuildCount = 0L;
        this.worldDirty = false;
    }

    public void markWorldInvalidated(String source) {
        this.worldDirty = true;
        if (source != null && !source.isBlank()) {
            this.recentEvents.put("world:" + source, ShopSignDiagnosticReason.DIRTY_REPARSE);
        }
    }

    public boolean isWorldDirty() {
        return this.worldDirty;
    }

    public void clearWorldDirty() {
        this.worldDirty = false;
    }

    public RefreshJob beginRefresh(MinecraftClient client, ShopSignParser parser, ParserRulesConfig rules, long tick, ShopSignDiagnosticReason reason) {
        ClientWorld world = client.world;
        if (world == null || client.player == null || parser == null) {
            this.activeJob = new RefreshJob(parser, rules, tick, reason, List.of());
            return this.activeJob;
        }

        long collectionStartedAt = System.nanoTime();
        Map<Long, WorldChunk> loadedChunks = this.collectLoadedChunks(client);
        SignContainerRelationResolver relationResolver = new SignContainerRelationResolver(rules);
        List<RefreshCandidate> candidates = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (WorldChunk chunk : loadedChunks.values()) {
            for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                if (!(entry.getValue() instanceof SignBlockEntity signBlockEntity)) {
                    continue;
                }

                BlockPos pos = entry.getKey();
                BlockState state = world.getBlockState(pos);
                boolean hanging = state.getBlock() instanceof HangingSignBlock;
                this.collectSnapshotCandidate(world, pos, new SignTextSnapshot(this.extractLines(signBlockEntity.getFrontText().getMessages(false)), hanging, SignSide.FRONT), relationResolver, rules, reason, seenKeys, candidates);
                this.collectSnapshotCandidate(world, pos, new SignTextSnapshot(this.extractLines(signBlockEntity.getBackText().getMessages(false)), hanging, SignSide.BACK), relationResolver, rules, reason, seenKeys, candidates);
            }
        }

        this.lastCollectedChunkCount = loadedChunks.size();
        this.lastCollectedSignCount = candidates.size();
        this.lastCollectionPhaseNanos = System.nanoTime() - collectionStartedAt;
        this.totalCollectionPhaseNanos += this.lastCollectionPhaseNanos;
        this.lastProcessedSigns = 0;
        this.activeJob = new RefreshJob(parser, rules, tick, reason, candidates);
        return this.activeJob;
    }

    public void detachJob(RefreshJob job) {
        if (this.activeJob == job) {
            this.activeJob = null;
        }
    }

    public int lastCollectedChunkCount() {
        return this.lastCollectedChunkCount;
    }

    public int lastCollectedSignCount() {
        return this.lastCollectedSignCount;
    }

    public DebugSnapshot debugSnapshot() {
        List<String> recent = this.recentEvents.entrySet().stream()
                .map(entry -> entry.getKey() + " -> " + entry.getValue().name())
                .toList();
        return new DebugSnapshot(
                this.negativeCache.snapshot(),
                recent,
                0,
                this.activeJob == null ? 0 : this.activeJob.remainingCandidates(),
                this.lastCollectedChunkCount,
                this.lastCollectedSignCount,
                0,
                this.lastProcessedSigns,
                this.lastCollectionPhaseNanos,
                this.lastSignPhaseNanos,
                this.totalCollectionPhaseNanos,
                this.totalSignPhaseNanos,
                this.cacheHitCount,
                this.negativeCacheHitCount,
                this.dirtyReparseCount,
                this.fullRebuildCount
        );
    }

    private void collectSnapshotCandidate(
            ClientWorld world,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelationResolver relationResolver,
            ParserRulesConfig rules,
            ShopSignDiagnosticReason reason,
            Set<String> seenKeys,
            List<RefreshCandidate> candidates
    ) {
        if (!hasMeaningfulText(snapshot.lines())) {
            return;
        }
        Identifier dimensionId = world.getRegistryKey().getValue();
        String cacheKey = dimensionId + "|" + pos.toShortString() + "|" + snapshot.side().name();
        if (!seenKeys.add(cacheKey)) {
            return;
        }
        SignContainerRelation relation = relationResolver.resolve(world, pos, snapshot, rules.barrel_search_radius);
        candidates.add(new RefreshCandidate(cacheKey, dimensionId, pos.toImmutable(), snapshot, relation, reason));
    }

    private Map<Long, WorldChunk> collectLoadedChunks(MinecraftClient client) {
        Map<Long, WorldChunk> chunks = new LinkedHashMap<>();
        if (client.world == null || client.player == null) {
            return chunks;
        }

        int viewDistance = client.options.getViewDistance().getValue();
        ChunkPos centerChunk = client.player.getChunkPos();
        for (int chunkX = centerChunk.x - viewDistance; chunkX <= centerChunk.x + viewDistance; chunkX++) {
            for (int chunkZ = centerChunk.z - viewDistance; chunkZ <= centerChunk.z + viewDistance; chunkZ++) {
                WorldChunk chunk = client.world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk != null) {
                    chunks.put(ChunkPos.toLong(chunkX, chunkZ), chunk);
                }
            }
        }
        return chunks;
    }

    private List<String> extractLines(Text[] messages) {
        List<String> lines = new ArrayList<>(messages.length);
        for (Text message : messages) {
            lines.add(message == null ? "" : message.getString());
        }
        return lines;
    }

    private static boolean hasMeaningfulText(List<String> lines) {
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return true;
            }
        }
        return false;
    }

    public final class RefreshJob {
        private final ShopSignParser parser;
        private final ParserRulesConfig rules;
        private final long tick;
        private final ShopSignDiagnosticReason reason;
        private final List<RefreshCandidate> candidates;
        private final Map<String, ShopSignEntry> entries = new LinkedHashMap<>();
        private int nextCandidateIndex;
        private boolean done;

        private RefreshJob(ShopSignParser parser, ParserRulesConfig rules, long tick, ShopSignDiagnosticReason reason, List<RefreshCandidate> candidates) {
            this.parser = parser;
            this.rules = rules;
            this.tick = tick;
            this.reason = reason;
            this.candidates = List.copyOf(candidates);
            this.done = this.candidates.isEmpty();
        }

        public void step(int signBudget) {
            if (this.done || this.parser == null) {
                this.done = true;
                return;
            }

            long startedAt = System.nanoTime();
            int processed = 0;
            while (this.nextCandidateIndex < this.candidates.size() && processed < Math.max(1, signBudget)) {
                RefreshCandidate candidate = this.candidates.get(this.nextCandidateIndex++);
                ShopSignFingerprint fingerprint = ShopSignFingerprint.create(candidate.dimensionId(), candidate.pos(), candidate.snapshot(), candidate.relation(), this.rules);

                ShopSignParseCache.CachedEntry cachedEntry = parseCache.get(candidate.cacheKey());
                if (cachedEntry != null && cachedEntry.fingerprint().equals(fingerprint)) {
                    this.entries.put(candidate.cacheKey(), cachedEntry.entry());
                    recentEvents.put(candidate.cacheKey(), ShopSignDiagnosticReason.CACHE_HIT);
                    cacheHitCount++;
                    processed++;
                    continue;
                }

                NegativeShopCache.NegativeEntry negativeEntry = negativeCache.get(candidate.cacheKey());
                if (negativeEntry != null && negativeEntry.fingerprint().equals(fingerprint)) {
                    if (negativeEntry.cachedEntry() != null) {
                        this.entries.put(candidate.cacheKey(), negativeEntry.cachedEntry());
                    } else {
                        this.entries.remove(candidate.cacheKey());
                    }
                    recentEvents.put(candidate.cacheKey(), ShopSignDiagnosticReason.NEGATIVE_CACHE_HIT);
                    negativeCacheHitCount++;
                    processed++;
                    continue;
                }

                ShopSignParseResult result = this.parser.parsePrepared(
                        candidate.dimensionId(),
                        candidate.pos(),
                        candidate.snapshot(),
                        candidate.relation(),
                        this.tick,
                        candidate.reason()
                );
                if (candidate.reason() == ShopSignDiagnosticReason.DIRTY_REPARSE) {
                    dirtyReparseCount++;
                }
                if (candidate.reason() == ShopSignDiagnosticReason.FULL_REBUILD) {
                    fullRebuildCount++;
                }

                if (!result.isShop() || result.entry() == null) {
                    this.entries.remove(candidate.cacheKey());
                    parseCache.remove(candidate.cacheKey());
                    negativeCache.put(candidate.cacheKey(), result.fingerprint(), result.reason(), String.join(" | ", candidate.snapshot().lines()));
                    recentEvents.put(candidate.cacheKey(), result.reason());
                } else {
                    this.entries.put(candidate.cacheKey(), result.entry());
                    parseCache.put(candidate.cacheKey(), result.fingerprint(), result.entry());
                    if (result.reason() == ShopSignDiagnosticReason.CONFIRMED_UNRESOLVABLE) {
                        negativeCache.put(candidate.cacheKey(), result.fingerprint(), result.reason(), String.join(" | ", candidate.snapshot().lines()), result.entry());
                        recentEvents.put(candidate.cacheKey(), result.reason());
                    } else {
                        negativeCache.remove(candidate.cacheKey());
                        recentEvents.put(candidate.cacheKey(), candidate.reason());
                    }
                }
                processed++;
            }

            lastProcessedSigns = processed;
            lastSignPhaseNanos = System.nanoTime() - startedAt;
            totalSignPhaseNanos += lastSignPhaseNanos;
            if (this.nextCandidateIndex >= this.candidates.size()) {
                this.done = true;
            }
        }

        public boolean isDone() {
            return this.done;
        }

        public int processedCandidates() {
            return this.nextCandidateIndex;
        }

        public int totalCandidates() {
            return this.candidates.size();
        }

        public int remainingCandidates() {
            return Math.max(0, this.candidates.size() - this.nextCandidateIndex);
        }

        public int foundEntries() {
            return this.entries.size();
        }

        public List<ShopSignEntry> entries() {
            return List.copyOf(this.entries.values());
        }
    }

    private record RefreshCandidate(
            String cacheKey,
            Identifier dimensionId,
            BlockPos pos,
            SignTextSnapshot snapshot,
            SignContainerRelation relation,
            ShopSignDiagnosticReason reason
    ) {
    }

    public record DebugSnapshot(
            List<NegativeShopCache.NegativeEntry> negativeEntries,
            List<String> recentEvents,
            int queuedChunks,
            int queuedSigns,
            int knownLoadedChunks,
            int knownSigns,
            int lastProcessedChunks,
            int lastProcessedSigns,
            long lastChunkPhaseNanos,
            long lastSignPhaseNanos,
            long totalChunkPhaseNanos,
            long totalSignPhaseNanos,
            long cacheHitCount,
            long negativeCacheHitCount,
            long dirtyReparseCount,
            long fullRebuildCount
    ) {
        public DebugSnapshot {
            negativeEntries = negativeEntries == null ? List.of() : List.copyOf(negativeEntries);
            recentEvents = recentEvents == null ? List.of() : List.copyOf(recentEvents);
        }
    }
}
