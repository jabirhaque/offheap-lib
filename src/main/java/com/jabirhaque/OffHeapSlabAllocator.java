package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Stack;

public class OffHeapSlabAllocator {

    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final long blockCount;

    private Stack<Long> freeBlocks = new Stack<>();

    public OffHeapSlabAllocator(long totalSize, long blockSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = totalSize/blockSize;
        initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = 64;
        this.blockCount = totalSize/blockSize;
        initialiseBlocks();
    }

    public OffHeapSlabAllocator() throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = 16 * 1024 * 1024;
        this.blockSize = 64;
        this.blockCount = totalSize/blockSize;
        initialiseBlocks();
    }

    private void initialiseBlocks(){
        long baseAddress = unsafe.allocateMemory(totalSize);
        long current = baseAddress;
        long limit = baseAddress+totalSize;
        while (current+blockSize <= limit ){
            freeBlocks.push(current);
            current += blockSize;
        }
    }

    public long allocate(long bytes) throws IllegalArgumentException {
        if (bytes > blockSize){
            throw new IllegalArgumentException("Requested size exceeds block size");
        }
        return allocateBlock();
    }

    private long allocateBlock(){
        if (freeBlocks.empty()) throw new OutOfMemoryError("Out of blocks");
        return freeBlocks.pop();
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