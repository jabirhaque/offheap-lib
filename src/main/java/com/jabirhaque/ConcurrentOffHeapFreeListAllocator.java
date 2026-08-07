package com.jabirhaque;

import sun.misc.Unsafe;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentOffHeapFreeListAllocator implements ConcurrentOffHeapAllocator{

    private OffHeapFreeListAllocator[] offHeapFreeListAllocators;

    private final Unsafe unsafe;
    private final long totalSize;
    private final long minSize;
    private final long baseAddress;
    private final int allocatorCount;

    private boolean closed = false;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentOffHeapFreeListAllocator(long totalSize, long minSize, int allocatorCount) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.allocatorCount = allocatorCount;
        validateInputs();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        try{
            initiateAllocators();
        }catch (Exception e){
            unsafe.freeMemory(this.baseAddress);
            throw new IllegalStateException("Failed to initiate allocators", e);
        }
    }

    public ConcurrentOffHeapFreeListAllocator(long totalSize, long minSize, int allocatorCount, Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.allocatorCount = allocatorCount;
        validateInputs();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        try{
            initiateAllocators();
        }catch (Exception e){
            unsafe.freeMemory(this.baseAddress);
            throw new IllegalStateException("Failed to initiate allocators", e);
        }
    }

    private void validateInputs(){
        if (totalSize < allocatorCount) throw new IllegalArgumentException("Total size must be greater than allocator count");
        if (allocatorCount <= 0) throw new IllegalArgumentException("Allocator count must be at least one");
    }

    private void initiateAllocators() throws NoSuchFieldException, IllegalAccessException {
        long allocatorSize = totalSize/allocatorCount;
        offHeapFreeListAllocators = new OffHeapFreeListAllocator[allocatorCount];
        for (int i=0; i<allocatorCount; i++){
            offHeapFreeListAllocators[i] = new OffHeapFreeListAllocator(allocatorSize, minSize, unsafe, baseAddress+i*allocatorSize);
        }
    }


    @Override
    public long allocate(long bytes) {
        lock.readLock().lock();
        try{
            if (closed){
                throw new IllegalStateException("Allocator closed");
            }
            int index = Math.floorMod(Long.hashCode(Thread.currentThread().getId()), allocatorCount);
            OffHeapFreeListAllocator allocator = offHeapFreeListAllocators[index];
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
            if (!validateAddress(address)) throw new IllegalArgumentException("Provided address is invalid");
            long allocatorSize = totalSize/allocatorCount;
            int index = (int)((address-baseAddress)/allocatorSize);
            OffHeapFreeListAllocator allocator = offHeapFreeListAllocators[index];
            allocator.free(address);
        }finally{
            lock.readLock().unlock();
        }
    }

    private boolean validateAddress(long address){
        long allocatorSize = totalSize/allocatorCount;
        int index = (int)((address-baseAddress)/allocatorSize);
        return index>=0 && index<allocatorCount;
    }

    @Override
    public void writeInt(long address, long offset, int val){
        lock.readLock().lock();
        try{
            if (closed){
                throw new IllegalStateException("Allocator closed");
            }
            if (!validateAddress(address)) throw new IllegalArgumentException("Provided address is invalid");
            long allocatorSize = totalSize/allocatorCount;
            int index = (int)((address-baseAddress)/allocatorSize);
            OffHeapFreeListAllocator allocator = offHeapFreeListAllocators[index];
            allocator.writeInt(address, offset, val);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public int readInt(long address, long offset){
        lock.readLock().lock();
        try{
            if (closed){
                throw new IllegalStateException("Allocator closed");
            }
            if (!validateAddress(address)) throw new IllegalArgumentException("Provided address is invalid");
            long allocatorSize = totalSize/allocatorCount;
            int index = (int)((address-baseAddress)/allocatorSize);
            OffHeapFreeListAllocator allocator = offHeapFreeListAllocators[index];
            return allocator.readInt(address, offset);
        }finally{
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws Exception {
        lock.writeLock().lock();
        try{
            if (closed) return;
            for (OffHeapFreeListAllocator allocator: offHeapFreeListAllocators){
                if (allocator.allocated()) throw new IllegalStateException("Cannot close allocator, blocks still allocated");
            }
            closed = true;
            unsafe.freeMemory(baseAddress);
        }finally{
            lock.writeLock().unlock();
        }
    }

    @Override
    public AllocationStatistics getAllocationStatisticsSnapshot(){
        lock.readLock().lock();
        try{
            AllocationStatistics allocationStatisticsSnapshot = new AllocationStatistics(totalSize);
            for (OffHeapFreeListAllocator allocator: offHeapFreeListAllocators){
                AllocationStatistics allocationStatistics = allocator.getAllocationStatisticsSnapshot();
                allocationStatisticsSnapshot.setAllocations(allocationStatisticsSnapshot.getAllocations()+allocationStatistics.getAllocations());
                allocationStatisticsSnapshot.setFrees(allocationStatisticsSnapshot.getFrees()+allocationStatistics.getFrees());
                allocationStatisticsSnapshot.setActiveAllocations(allocationStatisticsSnapshot.getActiveAllocations()+allocationStatistics.getActiveAllocations());
                allocationStatisticsSnapshot.setFailedAllocations(allocationStatisticsSnapshot.getFailedAllocations()+allocationStatistics.getFailedAllocations());
                allocationStatisticsSnapshot.setBytesAllocated(allocationStatisticsSnapshot.getBytesAllocated()+allocationStatistics.getBytesAllocated());
            }
            return allocationStatisticsSnapshot;
        }finally{
            lock.readLock().unlock();
        }
    }
}
