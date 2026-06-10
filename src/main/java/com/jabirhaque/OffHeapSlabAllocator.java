package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class OffHeapSlabAllocator implements OffHeapAllocator{

    private final Unsafe unsafe;
    protected final long totalSize;
    private final long blockSize;
    private final int blockCount;
    private final long baseAddress;

    private boolean closed = false;

    private int[] freeBlocks;
    private boolean[] allocatedSet;
    private int top;

    public OffHeapSlabAllocator(long totalSize, long blockSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize, long blockSize, Unsafe unsafe){
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        initialiseBlocks();
    }

    public OffHeapSlabAllocator(long baseAddress, long totalSize, long blockSize, Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = baseAddress;
        initialiseBlocks();
    }

    private int validateAndReturnCount(){
        if (blockSize>totalSize) throw new IllegalArgumentException("Block size cannot be greater than total size");
        if (totalSize <= 0 || blockSize <= 0) throw new IllegalArgumentException("Total and block size must be at least one byte");
        long count = totalSize / blockSize;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("Block count exceeds limit");
        return (int)count;
    }

    private void initialiseBlocks(){
        freeBlocks = new int[blockCount];
        for (int i=0; i<blockCount; i++) freeBlocks[i] = i;
        allocatedSet = new boolean[blockCount];
        top = blockCount-1;
    }

    @Override
    public synchronized long allocate(long bytes){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (bytes > blockSize) {
            throw new IllegalArgumentException("Requested size exceeds block size");
        }
        return allocateBlock();
    }

    private long allocateBlock(){
        if (top < 0) throw new OutOfMemoryError("Out of blocks");
        int index = freeBlocks[top--];
        allocatedSet[index] = true;
        return baseAddress+index*blockSize;
    }

    @Override
    public synchronized void free(long address){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (!validateAddress(address)) throw new IllegalArgumentException("Provided address is invalid");
        int index = (int)((address-baseAddress)/blockSize);
        unsafe.setMemory(address, blockSize, (byte)0);
        allocatedSet[index] = false;
        freeBlocks[++top] = index;
    }

    private boolean validateAddress(long address){
        long offset = address - baseAddress;
        if (offset%blockSize != 0) return false;
        int index = (int)(offset/blockSize);
        return index>=0 && index<blockCount && allocatedSet[index];
    }

    @Override
    public synchronized void close(){
        if (closed) return;

        if (allocated()){
            throw new IllegalStateException("Cannot close allocator: " + (blockCount - top - 1) + " blocks still allocated");
        }

        closed = true;
        unsafe.freeMemory(baseAddress);
    }

    public boolean allocated(){
        return top != blockCount-1;
    }
    

    public void printInfo(){
        System.out.println("Address size: " + unsafe.addressSize());
        System.out.println("Page size: " + unsafe.pageSize());
    }
}