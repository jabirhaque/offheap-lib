package com.jabirhaque;

import sun.misc.Unsafe;

import java.util.Map;

public class OffHeapFreeListAllocator implements OffHeapAllocator{

    private final Unsafe unsafe;
    private final long totalSize;
    private final long minSize;
    private final long baseAddress;

    private boolean closed = false;

    private final AllocationStatistics allocationStatistics;

    public OffHeapFreeListAllocator(long totalSize, long minSize) throws NoSuchFieldException, IllegalAccessException {
        this.unsafe = OffHeapAllocator.getUnsafe();
        this.totalSize = totalSize;
        this.minSize = minSize;
        this.baseAddress = unsafe.allocateMemory(totalSize);
        this.allocationStatistics = new AllocationStatistics(totalSize);
    }

    @Override
    public long allocate(long bytes){
        long freeOffset = 0;
        long sizeOffset = Byte.BYTES;
        long prevOffset = Byte.BYTES + Long.BYTES;
        long nextOffset = Byte.BYTES + 2*Long.BYTES;
        long headerSize = Byte.BYTES + 3*Long.BYTES;
        long current = baseAddress;
        while (current < baseAddress + totalSize && (unsafe.getByte(current + freeOffset) == 0 || unsafe.getLong(current + sizeOffset) < bytes)){
            current = unsafe.getLong(current + nextOffset);
        }
        if (current >= baseAddress + totalSize) throw new OutOfMemoryError("No blocks fit request");
        unsafe.putByte(current + freeOffset, (byte) 0);
        if (unsafe.getLong(current + sizeOffset) >= bytes + headerSize + minSize){
            long newHeader = current + headerSize + bytes;
            long size = unsafe.getLong(current + sizeOffset);
            long newSize = size - bytes - headerSize;

            unsafe.putByte(newHeader + freeOffset, (byte) 1);
            unsafe.putLong(newHeader + sizeOffset, newSize);
            unsafe.putLong(newHeader + prevOffset, current);
            unsafe.putLong(newHeader + nextOffset, unsafe.getLong(current + nextOffset));

            unsafe.putLong(current + sizeOffset, bytes);
            unsafe.putLong(current + nextOffset, newHeader);
            if (unsafe.getLong(newHeader + nextOffset) < baseAddress + totalSize){
                unsafe.putLong(unsafe.getLong(newHeader + nextOffset) + prevOffset, newHeader);
            }
        }
        return current + headerSize;
    }

    @Override
    public void free(long address){
    }

    @Override
    public void close(){
    }

    @Override
    public AllocationStatistics getAllocationStatisticsSnapshot(){
        return new AllocationStatistics(0);
    }
}
