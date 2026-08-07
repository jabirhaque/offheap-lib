package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public interface OffHeapAllocator extends AutoCloseable{
    long allocate(long bytes);
    void free(long address);
    void writeInt(long address, long offset, int value);
    int readInt(long address, long offset);
    AllocationStatistics getAllocationStatisticsSnapshot();

    static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }
}
