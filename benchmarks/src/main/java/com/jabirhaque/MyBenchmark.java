package com.jabirhaque;

import org.openjdk.jmh.annotations.*;

public class MyBenchmark {

    @State(Scope.Thread)
    public static class FreeState {

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
    public static class AllocateState {

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

    @Benchmark
    public void free(FreeState state) {
        state.allocator.free(state.address);
    }

    @Benchmark
    public void allocate(AllocateState state) {
        state.address = state.allocator.allocate(64);
    }
}