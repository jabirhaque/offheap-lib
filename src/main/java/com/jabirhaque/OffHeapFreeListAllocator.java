package com.jabirhaque;

import sun.misc.Unsafe;

public class OffHeapFreeListAllocator implements OffHeapAllocator{

    private static final long MAGIC = 0xCAFEBABEL;
    private static final long MAGIC_OFFSET = 0;
    private static final long FREE_OFFSET = Long.BYTES;
    private static final long SIZE_OFFSET = Long.BYTES + Byte.BYTES;
    private static final long PREV_OFFSET = Byte.BYTES + 2*Long.BYTES;
    private static final long NEXT_OFFSET = Byte.BYTES + 3*Long.BYTES;
    private static final long HEADER_SIZE = Byte.BYTES + 4*Long.BYTES;

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
        while (current < baseAddress + totalSize && (unsafe.getByte(current + FREE_OFFSET) == 0 || unsafe.getLong(current + SIZE_OFFSET) < bytes)){
            current = unsafe.getLong(current + NEXT_OFFSET);
        }
        if (current >= baseAddress + totalSize) throw new OutOfMemoryError("No blocks fit request");
        unsafe.putByte(current + FREE_OFFSET, (byte) 0);
        if (unsafe.getLong(current + SIZE_OFFSET) >= bytes + HEADER_SIZE + minSize){
            long newHeader = current + HEADER_SIZE + bytes;
            long size = unsafe.getLong(current + SIZE_OFFSET);
            long newSize = size - bytes - HEADER_SIZE;

            unsafe.putByte(newHeader + FREE_OFFSET, (byte) 1);
            unsafe.putLong(newHeader + SIZE_OFFSET, newSize);
            unsafe.putLong(newHeader + PREV_OFFSET, current);
            unsafe.putLong(newHeader + NEXT_OFFSET, unsafe.getLong(current + NEXT_OFFSET));

            unsafe.putLong(current + SIZE_OFFSET, bytes);
            unsafe.putLong(current + NEXT_OFFSET, newHeader);
            if (unsafe.getLong(newHeader + NEXT_OFFSET) < baseAddress + totalSize){
                unsafe.putLong(unsafe.getLong(newHeader + NEXT_OFFSET) + PREV_OFFSET, newHeader);
            }
        }
        return current + HEADER_SIZE;
    }

    @Override
    public void free(long address){
        if (!validateAddress(address)) throw new IllegalArgumentException("Provided address is invalid");
        long header = address - HEADER_SIZE;
        unsafe.putByte(header + FREE_OFFSET, (byte) 1);

        long prevHeader = unsafe.getLong(header + PREV_OFFSET);
        long nextHeader = unsafe.getLong(header + NEXT_OFFSET);

        long size = unsafe.getLong(header + SIZE_OFFSET);

        if (nextHeader < baseAddress + totalSize && unsafe.getByte(nextHeader + FREE_OFFSET) == 1){
            size += HEADER_SIZE + unsafe.getLong(nextHeader + SIZE_OFFSET);
            unsafe.putLong(header + SIZE_OFFSET, size);

            long nextNextHeader = unsafe.getLong(nextHeader + NEXT_OFFSET);
            unsafe.putLong(header + NEXT_OFFSET, nextNextHeader);

            if (nextNextHeader < baseAddress + totalSize){
                unsafe.putLong(nextNextHeader + PREV_OFFSET, header);
            }

            nextHeader = nextNextHeader;
        }

        if (prevHeader > -1 && unsafe.getByte(prevHeader + FREE_OFFSET) == 1){
            long prevSize = unsafe.getLong(prevHeader + SIZE_OFFSET);
            unsafe.putLong(prevHeader + SIZE_OFFSET, prevSize + HEADER_SIZE + size);
            unsafe.putLong(prevHeader + NEXT_OFFSET, nextHeader);

            if (nextHeader < baseAddress + totalSize){
                unsafe.putLong(nextHeader + PREV_OFFSET, prevHeader);
            }
        }
    }

    private boolean validateAddress(long address){
        long header = address - HEADER_SIZE;
        if (header < baseAddress || header >= baseAddress + totalSize) return false;
        if (unsafe.getLong(header + MAGIC_OFFSET) != MAGIC) return false;
        if (unsafe.getByte(header + FREE_OFFSET) == 1) return false;
        return true;
    }

    @Override
    public void close(){
    }

    @Override
    public AllocationStatistics getAllocationStatisticsSnapshot(){
        return new AllocationStatistics(0);
    }
}
