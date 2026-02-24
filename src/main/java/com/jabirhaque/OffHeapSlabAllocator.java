package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class OffHeapSlabAllocator implements AutoCloseable{

    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final int blockCount;
    private final long baseAddress;

    private boolean closed = false;

    private int[] freeBlocks;
    private boolean[] allocatedSet;
    private int top;

    public OffHeapSlabAllocator(long totalSize, long blockSize) throws NoSuchFieldException, IllegalAccessException {
        if (blockSize>totalSize){
            throw new IllegalArgumentException("Block size cannot be greater than total size");
        }
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = (int)(totalSize/blockSize);
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize) throws NoSuchFieldException, IllegalAccessException {
        if (64>totalSize){
            throw new IllegalArgumentException("Block size cannot be greater than total size");
        }
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = 64;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator() throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = 16 * 1024 * 1024;
        this.blockSize = 64;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = unsafe;
        this.totalSize = 16 * 1024 * 1024;
        this.blockSize = 64;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize, long blockSize, Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        if (blockSize>totalSize){
            throw new IllegalArgumentException("Block size cannot be greater than total size");
        }
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = validateAndReturnCount();
        this.baseAddress = initialiseBlocks();
    }

    private int validateAndReturnCount(){
        long count = totalSize / blockSize;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("Block count exceeds limit");
        return (int)count;
    }

    private long initialiseBlocks(){
        long address = unsafe.allocateMemory(totalSize);

        freeBlocks = new int[blockCount];
        for (int i=0; i<freeBlocks.length; i++) freeBlocks[i] = i;
        allocatedSet = new boolean[blockCount];
        top = blockCount-1;

        return address;
    }

    public long allocate(long bytes) throws IllegalArgumentException {
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

    public void free(long address){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (address < baseAddress || address >= baseAddress+totalSize || (address-baseAddress)%blockSize != 0) throw new IllegalArgumentException("Provided address is invalid");
        int index = (int)((address-baseAddress)/blockSize);
        if (!allocatedSet[index]) throw new IllegalArgumentException("Provided address is already free");
        unsafe.setMemory(address, blockSize, (byte)0);
        allocatedSet[index] = false;
        freeBlocks[++top] = index;
    }

    public void close(){
        if (closed) return;

        if (top != blockCount-1){
            throw new IllegalStateException("Cannot close allocator: " + (blockCount - top - 1) + " blocks still allocated");
        }

        unsafe.freeMemory(baseAddress);
        closed = true;
    }

    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    public void printInfo(){
        System.out.println("Address size: " + unsafe.addressSize());
        System.out.println("Page size: " + unsafe.pageSize());
    }
}