package com.jabirhaque;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;

public class MyBenchmark {

    @State(Scope.Thread)
    public static class SlabFreeState {

        OffHeapSlabAllocator allocator;
        long address;
        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            long totalSize = 16 * 1024 * 1024;
            long blockSize = 64;
            int blockCount = (int)(totalSize/blockSize);
            allocator = new OffHeapSlabAllocator(totalSize, blockSize);

            int allocatedCount = (int) (Math.random() * (blockCount/4));
            for (int i=0; i<allocatedCount; i++){
                addresses.add(allocator.allocate(64));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @Setup(Level.Invocation)
        public void prepareFree() {
            address = allocator.allocate(64);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses) allocator.free(address);
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class SlabAllocateState {

        OffHeapSlabAllocator allocator;
        long address;
        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            long totalSize = 16 * 1024 * 1024;
            long blockSize = 64;
            int blockCount = (int)(totalSize/blockSize);
            allocator = new OffHeapSlabAllocator(totalSize, blockSize);

            int allocatedCount = (int) (Math.random() * (blockCount/4));
            for (int i=0; i<allocatedCount; i++){
                addresses.add(allocator.allocate(64));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @TearDown(Level.Invocation)
        public void completeAllocate() {
            allocator.free(address);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses) allocator.free(address);
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class BuddyFreeState {

        long totalSize = 16 * 1024 * 1024;
        long minSize = 64;
        int blockCount = (int)(totalSize/minSize);
        long[] sizes = {64, 128, 256};

        OffHeapBuddyAllocator allocator;
        long address;

        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapBuddyAllocator(totalSize, minSize);

            int allocatedCount = (int) (Math.random() * (blockCount/16));
            for (int i=0; i<allocatedCount; i++){
                long size = sizes[(int)(Math.random()*3)];
                addresses.add(allocator.allocate(size));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @Setup(Level.Invocation)
        public void prepareFree() {
            long size = sizes[(int)(Math.random()*3)];
            address = allocator.allocate(size);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses){
                allocator.free(address);
            }
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class BuddyAllocateState {

        long totalSize = 16 * 1024 * 1024;
        long minSize = 64;
        int blockCount = (int)(totalSize/minSize);
        long[] sizes = {64, 128, 256};

        OffHeapBuddyAllocator allocator;
        long address;
        long size;

        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapBuddyAllocator(totalSize, minSize);

            int allocatedCount = (int) (Math.random() * (blockCount/16));
            for (int i=0; i<allocatedCount; i++){
                long size = sizes[(int)(Math.random()*3)];
                addresses.add(allocator.allocate(size));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @Setup(Level.Invocation)
        public void prepareALlocate() {
            size = sizes[(int)(Math.random()*3)];
        }

        @TearDown(Level.Invocation)
        public void completeAllocate() {
            allocator.free(address);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses){
                allocator.free(address);
            }
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class FreeListFreeState {

        long totalSize = 16 * 1024 * 1024;
        long minSize = 64;
        int blockCount = (int)(totalSize/minSize);

        OffHeapFreeListAllocator allocator;
        long address;

        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapFreeListAllocator(totalSize, minSize);

            int allocatedCount = (int) (Math.random() * (blockCount/16));
            for (int i=0; i<allocatedCount; i++){
                long size = (long)(Math.random()*128)+1;
                addresses.add(allocator.allocate(size));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @Setup(Level.Invocation)
        public void prepareFree() {
            long size = (long)(Math.random()*128);
            address = allocator.allocate(size);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses){
                allocator.free(address);
            }
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class FreeListAllocateState {

        long totalSize = 16 * 1024 * 1024;
        long minSize = 64;
        int blockCount = (int)(totalSize/minSize);

        OffHeapFreeListAllocator allocator;
        long address;
        long size;

        List<Long> addresses = new ArrayList<>();

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapFreeListAllocator(totalSize, minSize);

            int allocatedCount = (int) (Math.random() * (blockCount/16));
            for (int i=0; i<allocatedCount; i++){
                long size = (long)(Math.random()*128)+1;
                addresses.add(allocator.allocate(size));
            }

            int freeCount = (int) (Math.random() * allocatedCount);
            for (int i=0; i<freeCount; i++){
                int index = (int) (Math.random() * addresses.size());
                allocator.free(addresses.get(index));
                addresses.remove(index);
            }
        }

        @Setup(Level.Invocation)
        public void prepareALlocate() {
            size = (long)(Math.random()*128 + 1);
        }

        @TearDown(Level.Invocation)
        public void completeAllocate() {
            allocator.free(address);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            for (long address: addresses){
                allocator.free(address);
            }
            allocator.close();
        }
    }

    @Benchmark
    public void slabFree(SlabFreeState state) {
        state.allocator.free(state.address);
    }

    @Benchmark
    public void slabAllocate(SlabAllocateState state) {
        state.address = state.allocator.allocate(64);
    }

    @Benchmark
    public void buddyFree(BuddyFreeState state) {
        state.allocator.free(state.address);
    }

    @Benchmark
    public void buddyAllocate(BuddyAllocateState state) {
        state.address = state.allocator.allocate(state.size);
    }

    @Benchmark
    public void freeListFree(FreeListFreeState state) {
        state.allocator.free(state.address);
    }

    @Benchmark
    public void freeListAllocate(FreeListAllocateState state) {
        state.address = state.allocator.allocate(state.size);
    }
}