package com.jabirhaque;

import sun.misc.Unsafe;

public class ConcurrentOffHeapSlabAllocator implements OffHeapAllocator{

    private OffHeapSlabAllocator[] offHeapAllocators;

    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final long baseAddress;
    private final int allocatorCount;

    private boolean closed = false;

    public ConcurrentOffHeapSlabAllocator(long totalSize, long blockSize, int allocatorCount) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.baseAddress = unsafe.allocateMemory(totalSize);
        this.allocatorCount = allocatorCount;
        initiateAllocators();
    }

    private void validateInputs(){
        if (totalSize < allocatorCount) throw new IllegalArgumentException("Total size must be greater than allocator count");
        if (allocatorCount <= 0) throw new IllegalArgumentException("Allocator count must be at least one");
    }

    private void initiateAllocators(){
        long allocatorSize = totalSize/allocatorCount;
        offHeapAllocators = new OffHeapSlabAllocator[allocatorCount];
        for (int i=0; i<allocatorCount; i++){
            offHeapAllocators[i] = new OffHeapSlabAllocator(baseAddress+i*allocatorSize, allocatorSize, blockSize);
        }
    }


    @Override
    public long allocate(long bytes) {
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        int index = (int)Thread.currentThread().getId()%allocatorCount;
        OffHeapSlabAllocator allocator = offHeapAllocators[index];
        return allocator.allocate(bytes);
    }

    @Override
    public void free(long address) {
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (!owns(address)) throw new IllegalArgumentException("Provided address is invalid");
        int index = (int)(address-baseAddress)%allocatorCount;
        OffHeapSlabAllocator allocator = offHeapAllocators[index];
        allocator.free(address);
    }

    @Override
    public boolean owns(long address) {
        int index = (int)(address-baseAddress)%allocatorCount;
        return index>=0 && index<allocatorCount && offHeapAllocators[index].owns(address);
    }

    @Override
    public void close() throws Exception {
        if (closed) return;
        for (OffHeapSlabAllocator allocator: offHeapAllocators) allocator.close();
        unsafe.freeMemory(baseAddress);
        closed = true;
    }
}
