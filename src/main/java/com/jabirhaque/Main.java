package com.jabirhaque;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(16 * 1024 * 1024, 64);
        offHeapSlabAllocator.printInfo();
    }
}
