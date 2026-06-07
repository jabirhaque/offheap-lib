package com.jabirhaque;

import sun.misc.Unsafe;

public class ConcurrentOffHeapSlabAllocator implements OffHeapAllocator{

    private OffHeapSlabAllocator[] offHeapAllocators;
    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final long baseAddress;
    private final int allocatorCount;

    public ConcurrentOffHeapSlabAllocator(long totalSize, long blockSize, int allocatorCount) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize; //TODO: ensure power of two
        this.baseAddress = unsafe.allocateMemory(totalSize);
        this.allocatorCount = allocatorCount;
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
        int index = (int)Thread.currentThread().getId()%allocatorCount;
        OffHeapSlabAllocator allocator = offHeapAllocators[index];
        return allocator.allocate(bytes);
    }

    @Override
    public void free(long address) {
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

    }
}
