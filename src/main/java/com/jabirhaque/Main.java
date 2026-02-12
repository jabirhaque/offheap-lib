package com.jabirhaque;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator();
        offHeapSlabAllocator.printInfo();
        long ptr = offHeapSlabAllocator.allocateMemory(1024);
        System.out.println(ptr);
    }
}
