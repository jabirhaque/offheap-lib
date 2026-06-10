package com.jabirhaque;

import sun.misc.Unsafe;

import java.util.HashMap;
import java.util.Map;

public class OffHeapBuddyAllocator implements OffHeapAllocator{

    private final Unsafe unsafe;
    private final long totalSize;
    private final long minSize;
    private final long baseAddress;

    private boolean closed = false;
    private final int levels;

    private long[][] freeLists;
    private int[] freeCounts;

    private Map<Long, Integer> allocatedMap;



    OffHeapBuddyAllocator(long totalSize, long minSize) throws NoSuchFieldException, IllegalAccessException {
        validate(totalSize, minSize);
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        initialiseBlocks();
    }

    OffHeapBuddyAllocator(long totalSize, long minSize, Unsafe unsafe){
        validate(totalSize, minSize);
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = unsafe.allocateMemory(totalSize);
        initialiseBlocks();
    }

    OffHeapBuddyAllocator(long baseAddress, long totalSize, long minSize, Unsafe unsafe){
        validate(totalSize, minSize);
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = baseAddress;
        initialiseBlocks();
    }

    private void validate(long totalSize, long minSize){
        if (minSize>totalSize) throw new IllegalArgumentException("Minimum block size cannot be greater than the total size");
        if (totalSize <= 0 | minSize <= 0) throw new IllegalArgumentException("Total and block size must be at least one byte");
        if (!powerOfTwo(totalSize) || !powerOfTwo(minSize)) throw new IllegalArgumentException("Both total size and minimum block size must be powers of two");
        long count = totalSize / minSize;
        if (count > Integer.MAX_VALUE) throw new IllegalArgumentException("Block count exceeds limit");
    }

    private void initialiseBlocks() {
        freeLists = new long[levels][];
        freeCounts = new int[levels];

        for (int level = 0; level < levels; level++) {
            long blockSize = minSize << level;
            int maxBlocks = (int)(totalSize / blockSize);
            freeLists[level] = new long[maxBlocks];
        }

        freeLists[levels - 1][0] = 0;
        freeCounts[levels - 1] = 1;
    }

    public long allocate(long bytes){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        if (bytes > totalSize){
            throw new IllegalArgumentException("Requested size exceeds total size");
        }
        int level = getLevel(bytes);
        long offset = (freeCounts[level] > 0) ? freeLists[level][--freeCounts[level]] : splitAndAllocate(level+1);
        allocatedMap.put(offset, level);
        return baseAddress+offset;
    }

    private long splitAndAllocate(int level){
        if (level == levels) throw new OutOfMemoryError("Out of blocks to fit this request");
        long offset = (freeCounts[level] > 0) ? freeLists[level][--freeCounts[level]] : splitAndAllocate(level+1);
        long buddyOffset = offset + (minSize<<(level-1));
        freeLists[level-1][freeCounts[level-1]++] = buddyOffset;
        return offset;
    }


    private int getLevel(long bytes) {
        if (bytes > totalSize) {
            throw new IllegalArgumentException("Request exceeds total memory");
        }
        if (bytes<=minSize) return 0;

        long blockSize = bytes;
        if (!powerOfTwo(blockSize)) {
            blockSize = 1L << (64 - Long.numberOfLeadingZeros(blockSize));
        }

        int i = Long.numberOfTrailingZeros(blockSize);
        int j = Long.numberOfTrailingZeros(minSize);
        int res = i - j;
        return res;
    }

    public void free(long address){
        if (closed){
            throw new IllegalStateException("Allocator closed");
        }
        long offset = address-baseAddress;
        if (!owns(address)) throw new IllegalArgumentException("Provided address is invalid");
        int level = allocatedMap.get(offset);
        allocatedMap.remove(offset);
        mergeAndFree(offset, level);
    }

    private void mergeAndFree(long offset, int level){
        long buddyOffset = offset ^ (minSize << level);
        int index = freeCounts[level];
        for (int i=0; i<freeCounts[level]; i++){
            if (freeLists[level][i] == buddyOffset){
                index = i;
                break;
            }
        }
        if (index == freeCounts[level]){
            freeLists[level][freeCounts[level]++] = offset;
            unsafe.setMemory(baseAddress+offset, minSize<<level , (byte)0);
            return;
        }
        freeLists[level][index] = freeLists[level][--freeCounts[level]];
        mergeAndFree(Math.min(offset, buddyOffset), level+1);
    }

    public boolean owns(long address){
        return allocatedMap.containsKey(address-baseAddress);
    }

    public void close(){
        if (closed) return;

        if (freeCounts[levels-1] == 0){
            throw new IllegalStateException("Cannot close allocator, blocks still allocated");
        }

        unsafe.freeMemory(baseAddress);
        closed = true;
    }

    public boolean allocated(){
        return freeCounts[levels-1] == 0;
    }

    private boolean powerOfTwo(long num){
        return (num & (num-1)) == 0;
    }
}
