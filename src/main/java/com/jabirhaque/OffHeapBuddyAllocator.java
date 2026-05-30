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
        if (minSize>totalSize){
            throw new IllegalArgumentException("Minimum block size cannot be greater than the total size");
        }
        if (!powerOfTwo(totalSize) || !powerOfTwo(minSize)){
            throw new IllegalArgumentException("Both total size and minimum block size must be powers of two");
        }
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = initialiseBlocks();
    }

    OffHeapBuddyAllocator(Unsafe unsafe){
        this.unsafe = unsafe;
        this.totalSize = 16 * 1024 * 1024;
        this.minSize = 64;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = initialiseBlocks();
    }

    OffHeapBuddyAllocator( long totalSize, long minSize, Unsafe unsafe){
        if (minSize>totalSize){
            throw new IllegalArgumentException("Minimum block size cannot be greater than the total size");
        }
        if (!powerOfTwo(totalSize) || !powerOfTwo(minSize)){
            throw new IllegalArgumentException("Both total size and minimum block size must be powers of two");
        }
        this.unsafe = unsafe;
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.allocatedMap = new HashMap<>();
        this.baseAddress = initialiseBlocks();
    }

    private long initialiseBlocks() {
        long address = unsafe.allocateMemory(totalSize);

        freeLists = new long[levels][];
        freeCounts = new int[levels];

        for (int level = 0; level < levels; level++) {
            long blockSize = minSize << level;
            int maxBlocks = (int)(totalSize / blockSize);
            freeLists[level] = new long[maxBlocks];
        }

        freeLists[levels - 1][0] = 0;
        freeCounts[levels - 1] = 1;

        return address;
    }

    public long allocate(long bytes){
        if (closed){
            throw new IllegalStateException("Allocator closed");
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
        //TODO: clean free'd blocks e.g. unsafe.setMemory(address, blockSize, (byte)0)
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

    private boolean powerOfTwo(long num){
        return (num & (num-1)) == 0;
    }
}
