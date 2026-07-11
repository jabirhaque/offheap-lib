package com.jabirhaque;

import org.openjdk.jmh.annotations.*;

public class MyBenchmark {

    @State(Scope.Thread)
    public static class SlabFreeState {

        OffHeapSlabAllocator allocator;
        long address;

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapSlabAllocator(
                    16 * 1024 * 1024,
                    64
            );
        }

        @Setup(Level.Invocation)
        public void prepareFree() {
            address = allocator.allocate(64);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class SlabAllocateState {

        OffHeapSlabAllocator allocator;
        long address;

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapSlabAllocator(
                    16 * 1024 * 1024,
                    64
            );
        }

        @TearDown(Level.Invocation)
        public void completeAllocate() {
            allocator.free(address);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class BuddyFreeState {

        OffHeapBuddyAllocator allocator;
        long address;

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapBuddyAllocator(
                    16 * 1024 * 1024,
                    64
            );
        }

        @Setup(Level.Invocation)
        public void prepareFree() {
            address = allocator.allocate(64);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            allocator.close();
        }
    }

    @State(Scope.Thread)
    public static class BuddyAllocateState {

        OffHeapBuddyAllocator allocator;
        long address;

        @Setup(Level.Trial)
        public void setup() throws NoSuchFieldException, IllegalAccessException {
            allocator = new OffHeapBuddyAllocator(
                    16 * 1024 * 1024,
                    64
            );
        }

        @TearDown(Level.Invocation)
        public void completeAllocate() {
            allocator.free(address);
        }

        @TearDown(Level.Trial)
        public void tearDown() {
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
        state.address = state.allocator.allocate(64);
    }
}