package com.jabirhaque;

import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
public class MyBenchmark {

    private OffHeapSlabAllocator allocator;
    private long address;

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

//    @Benchmark
//    public long allocate() {
//        long addr = allocator.allocate(64);
//        allocator.free(addr); // clean up
//        return addr;
//    }

    @Benchmark
    public void free() {
        allocator.free(address);
    }
}