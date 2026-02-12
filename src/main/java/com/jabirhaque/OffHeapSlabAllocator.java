package com.jabirhaque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class OffHeapSlabAllocator {

    private final Unsafe unsafe;

    public OffHeapSlabAllocator() throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        this.unsafe = (Unsafe) f.get(null);;
    }

    public void printInfo(){
        System.out.println("Address size: " + unsafe.addressSize());
        System.out.println("Page size: " + unsafe.pageSize());
    }
}
