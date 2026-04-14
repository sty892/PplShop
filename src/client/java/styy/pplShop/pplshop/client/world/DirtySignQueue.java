package styy.pplShop.pplshop.client.world;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import styy.pplShop.pplshop.client.model.ShopSignDiagnosticReason;
import styy.pplShop.pplshop.client.model.SignTextSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DirtySignQueue {
    private final Map<Long, ChunkTask> chunkTasks = new LinkedHashMap<>();
    private final Map<String, SignTask> signTasks = new LinkedHashMap<>();

    public void enqueueChunk(WorldChunk chunk, ShopSignDiagnosticReason reason) {
        if (chunk != null) {
            long key = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);
            this.chunkTasks.put(key, new ChunkTask(chunk, reason));
        }
    }

    public ChunkTask pollChunk() {
        if (this.chunkTasks.isEmpty()) {
            return null;
        }
        Long firstKey = this.chunkTasks.keySet().iterator().next();
        return this.chunkTasks.remove(firstKey);
    }

    public void enqueueSign(Identifier dimensionId, BlockPos pos, SignTextSnapshot snapshot, ShopSignDiagnosticReason reason) {
        String key = dimensionId + "|" + pos.asLong() + "|" + snapshot.side().name();
        this.signTasks.put(key, new SignTask(dimensionId, pos.toImmutable(), snapshot, reason));
    }

    public SignTask pollSign() {
        if (this.signTasks.isEmpty()) {
            return null;
        }
        String firstKey = this.signTasks.keySet().iterator().next();
        return this.signTasks.remove(firstKey);
    }

    public void clear() {
        this.chunkTasks.clear();
        this.signTasks.clear();
    }

    public boolean hasChunkTasks() {
        return !this.chunkTasks.isEmpty();
    }

    public boolean hasSignTasks() {
        return !this.signTasks.isEmpty();
    }

    public int chunkTaskCount() {
        return this.chunkTasks.size();
    }

    public int signTaskCount() {
        return this.signTasks.size();
    }

    public record ChunkTask(WorldChunk chunk, ShopSignDiagnosticReason reason) {
    }

    public record SignTask(Identifier dimensionId, BlockPos pos, SignTextSnapshot snapshot, ShopSignDiagnosticReason reason) {
    }
}
