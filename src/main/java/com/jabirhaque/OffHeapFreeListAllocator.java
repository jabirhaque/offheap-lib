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
        long current = baseAddress;
        while (current < baseAddress + totalSize && (unsafe.getByte(current) == 0 || unsafe.getLong(current + Byte.BYTES) < bytes)){
            current = unsafe.getLong(current + Byte.BYTES + 2*Long.BYTES);
        }
        if (current >= baseAddress + totalSize) throw new OutOfMemoryError("No blocks fit request");
        unsafe.putByte(current, (byte) 0);
        if (unsafe.getLong(current + Byte.BYTES) >= bytes + (Byte.BYTES+3*Long.BYTES) + minSize){
            unsafe.putByte(current + Byte.BYTES + 3*Long.BYTES + bytes, (byte) 1);
            unsafe.putLong(current + 2*Byte.BYTES + 3*Long.BYTES + bytes, unsafe.getLong(current + Byte.BYTES) - bytes - (Byte.BYTES + 3*Long.BYTES));
            unsafe.putLong(current + 2*Byte.BYTES + 4*Long.BYTES + bytes, current);
            unsafe.putLong(current + 2*Byte.BYTES + 5*Long.BYTES + bytes, unsafe.getLong(current + Byte.BYTES + 2*Long.BYTES));

            unsafe.putLong(current + Byte.BYTES + 2*Long.BYTES, current + Byte.BYTES + 3*Long.BYTES + bytes);
            if (unsafe.getLong(current + 2*Byte.BYTES + 5*Long.BYTES + bytes) < baseAddress + totalSize){
                unsafe.putLong(unsafe.getLong(current + 2*Byte.BYTES + 5*Long.BYTES + bytes) + Byte.BYTES + Long.BYTES, current + Byte.BYTES + 3*Long.BYTES + bytes);
            }
        }
        return current + Byte.BYTES + 3*Long.BYTES;
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
