package com.grimmauld.createintegration.misc;

import com.grimmauld.createintegration.Config;
import com.grimmauld.createintegration.CreateIntegration;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public class ChunkLoaderList implements IChunkLoaderList {
    @Nullable
    private final ServerWorld world;
    public HashMap<BlockPos, Integer> chunkLoaders;
    private boolean enabled = false;

    public ChunkLoaderList(@Nullable ServerWorld world) {
        this.world = world;
        chunkLoaders = new HashMap<>();
    }

    public static long toChunk(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public void resetForBlock(BlockPos pos) {
        if (contains(pos)) {
            chunkLoaders.put(pos, 5);
        } else {
            add(pos);
        }
    }

    @Override
    public void tickDown() {
        if (!chunkLoaders.isEmpty() && enabled) {
            for (BlockPos pos : chunkLoaders.keySet()) {
                if (chunkLoaders.get(pos) > -1) {  // prevent overflows
                    chunkLoaders.put(pos, chunkLoaders.get(pos) - 1);
                }
            }
        }
        update();
    }

    private void force(BlockPos pos) {
        forceload(pos, "add");
    }

    private void unforce(BlockPos pos) {
        forceload(pos, "remove");
    }

    private void forceload(BlockPos pos, String action) {
        if (this.world == null) return;

        CommandSource source = (this.world.getServer().getCommandSource().withWorld(this.world));
        if (!Config.CHUNK_CHAT.get()) {
            source = source.withFeedbackDisabled();
        }

        @SuppressWarnings("unused")
        int ret = this.world.getServer().getCommandManager().handleCommand(source, "forceload " + action + " " + pos.getX() + " " + pos.getZ());
    }

    @Override
    public void add(BlockPos pos) {
        chunkLoaders.put(pos, 5);
        force(pos);
        update();
    }

    @Override
    public void remove(BlockPos pos) {
        chunkLoaders.put(pos, 0);
        update();
    }

    public void addSilent(BlockPos pos) {
        chunkLoaders.put(pos, 5);
    }

    @Override
    public void start() {
        for (BlockPos pos : chunkLoaders.keySet()) {
            chunkLoaders.put(pos, 5);
            force(pos);
        }
        enabled = true;
    }

    private void update() {
        try {
            if (world != null && chunkLoaders != null) {
                if (!chunkLoaders.isEmpty()) {
                    for (BlockPos pos : chunkLoaders.keySet()) {
                        if (chunkLoaders.get(pos) <= 0) {  // TODO: only check 0 ?
                            chunkLoaders.remove(pos);
                            if (!getChunkNumbers().contains(toChunk(pos))) {
                                unforce(pos);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // CreateIntegration.logger.catching(e);
        }
    }

    public ArrayList<Long> getChunkNumbers() {
        ArrayList<Long> chunkNumbers = new ArrayList<>();
        if (!chunkLoaders.isEmpty()) {
            for (BlockPos pos : chunkLoaders.keySet()) {
                if (chunkLoaders.get(pos) > 0) {  // only active chunk loaders are saved
                    chunkNumbers.add(toChunk(pos));
                }
            }
        }
        return chunkNumbers;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return chunkLoaders.containsKey(pos) && chunkLoaders.get(pos) > 0;
    }

    public static class Storage implements IStorage<IChunkLoaderList> {
        @Override
        public INBT writeNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance, Direction side) {
            if (!(instance instanceof ChunkLoaderList)) return null;
            return new LongArrayNBT(((ChunkLoaderList) instance).getChunkNumbers());
        }

        @Override
        public void readNBT(Capability<IChunkLoaderList> capability, IChunkLoaderList instance, Direction side, INBT nbt) {
            if (!(instance instanceof ChunkLoaderList) || !(nbt instanceof LongArrayNBT)) return;
            ChunkLoaderList list = (ChunkLoaderList) instance;
            try {
                for (long l : ((LongArrayNBT) nbt).getAsLongArray()) {
                    list.addSilent(BlockPos.fromLong(l));
                }
            } finally {
                CreateIntegration.logger.debug("Loaded Chunk Loader positions.");
            }
        }
    }
}