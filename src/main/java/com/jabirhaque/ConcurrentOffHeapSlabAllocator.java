package com.jabirhaque;

import sun.misc.Unsafe;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentOffHeapSlabAllocator implements OffHeapAllocator{

    private OffHeapSlabAllocator[] offHeapAllocators;

    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final long baseAddress;
    private final int allocatorCount;

    private boolean closed = false; //TODO: make atomic
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentOffHeapSlabAllocator(long totalSize, long blockSize, int allocatorCount) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.allocatorCount = allocatorCount;
        validateInputs();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        initiateAllocators();
    }

    private void validateInputs(){
        if (totalSize < allocatorCount) throw new IllegalArgumentException("Total size must be greater than allocator count");
        if (allocatorCount <= 0) throw new IllegalArgumentException("Allocator count must be at least one");
    }

    private void initiateAllocators() throws NoSuchFieldException, IllegalAccessException {
        long allocatorSize = totalSize/allocatorCount;
        offHeapAllocators = new OffHeapSlabAllocator[allocatorCount];
        for (int i=0; i<allocatorCount; i++){
            offHeapAllocators[i] = new OffHeapSlabAllocator(baseAddress+i*allocatorSize, allocatorSize, blockSize);
        }
    }


    @Override
    public long allocate(long bytes) {
        lock.readLock().lock();
        try{
            if (closed){
                throw new IllegalStateException("Allocator closed");
            }
            int index = (int)(Thread.currentThread().getId()%allocatorCount); //TODO: investigate
            OffHeapSlabAllocator allocator = offHeapAllocators[index];
            return allocator.allocate(bytes);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public void free(long address) {
        lock.readLock().lock();
        try{
            if (closed){
                throw new IllegalStateException("Allocator closed");
            }
            if (!owns(address)) throw new IllegalArgumentException("Provided address is invalid");
            long allocatorSize = totalSize/allocatorCount;
            int index = (int)((address-baseAddress)/allocatorSize);
            OffHeapSlabAllocator allocator = offHeapAllocators[index];
            allocator.free(address);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean owns(long address) {
        long allocatorSize = totalSize/allocatorCount;
        int index = (int)((address-baseAddress)/allocatorSize);
        return index>=0 && index<allocatorCount && offHeapAllocators[index].owns(address);
    }

    @Override
    public void close() throws Exception {
        lock.writeLock().lock();
        try{
            if (closed) return;
            for (OffHeapSlabAllocator allocator: offHeapAllocators){
                if (allocator.allocated()) throw new IllegalStateException("Cannot close allocator, blocks still allocated");
            }
            closed = true;
            unsafe.freeMemory(baseAddress);
        }finally{
            lock.writeLock().unlock();
        }
    }
}
