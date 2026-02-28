package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public interface OffHeapAllocator extends AutoCloseable{
    long allocate(long bytes);
    void free(long address);
    boolean owns(long address);

    static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }
}
