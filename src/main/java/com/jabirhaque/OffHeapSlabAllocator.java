package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class OffHeapSlabAllocator {

    private final Unsafe unsafe;
    private final long totalSize;
    private final long blockSize;
    private final long blockCount;
    private final long baseAddress;

    private boolean closed = false;

    private Stack<Long> freeBlocks = new Stack<>();
    private Set<Long> freeSet = new HashSet<>();
    private Set<Long> allocatedSet = new HashSet<>();

    public OffHeapSlabAllocator(long totalSize, long blockSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = totalSize/blockSize;
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = totalSize;
        this.blockSize = 64;
        this.blockCount = totalSize/blockSize;
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator() throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = getUnsafe();
        this.totalSize = 16 * 1024 * 1024;
        this.blockSize = 64;
        this.blockCount = totalSize/blockSize;
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = unsafe;
        this.totalSize = 16 * 1024 * 1024;
        this.blockSize = 64;
        this.blockCount = totalSize/blockSize;
        this.baseAddress = initialiseBlocks();
    }

    public OffHeapSlabAllocator(long totalSize, long blockSize, Unsafe unsafe) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.blockSize = blockSize;
        this.blockCount = totalSize/blockSize;
        this.baseAddress = initialiseBlocks();
    }

    private long initialiseBlocks(){
        long address = unsafe.allocateMemory(totalSize);

        long current = address;
        long limit = address+totalSize;
        while (current+blockSize <= limit ){
            freeBlocks.push(current);
            freeSet.add(current);
            current += blockSize;
        }

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
        if (freeBlocks.empty()) throw new OutOfMemoryError("Out of blocks");
        long address = freeBlocks.pop();
        freeSet.remove(address);
        allocatedSet.add(address);
        return address;
    }

    public void free(long address){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (!allocatedSet.contains(address))  throw new IllegalArgumentException("Provided address is invalid");
        allocatedSet.remove(address);
        freeSet.add(address);
        freeBlocks.push(address);
    }

    public void close(){
        if (closed) return;
        unsafe.freeMemory(baseAddress);
        closed = true;

        freeBlocks.clear();
        freeSet.clear();
        allocatedSet.clear();
    }

    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    public int getAddressSize(){
        return unsafe.addressSize();
    }

    public int getPageSize(){
        return unsafe.pageSize();
    }

    public void printInfo(){
        System.out.println("Address size: " + unsafe.addressSize());
        System.out.println("Page size: " + unsafe.pageSize());
    }
}