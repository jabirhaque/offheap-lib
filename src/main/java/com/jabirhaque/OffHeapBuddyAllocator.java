package com.jabirhaque;

import sun.misc.Unsafe;

public class OffHeapBuddyAllocator implements OffHeapAllocator{

    private final Unsafe unsafe;
    private final long totalSize;
    private final long minSize;
    private final long address;

    private long[][] freeLists;
    private int[] freeCounts;
    private final int levels;

    OffHeapBuddyAllocator(long totalSize, long minSize) throws NoSuchFieldException, IllegalAccessException {
        if (minSize>totalSize){
            throw new IllegalArgumentException("Minimum block size cannot be greater than the total size");
        }//check power of two
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.levels = (int)(Math.log(totalSize/minSize)/Math.log(2))+1;
        this.address = initialiseBlocks();
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

    public long allocate(long bytes){return 0;}

    public void free(long address){}

    public boolean owns(long address){return true;}

    public void close(){}
}
