package com.jabirhaque;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator();
        offHeapSlabAllocator.printInfo();
    }
}
