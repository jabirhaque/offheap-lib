package com.jabirhaque;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        OffHeapBuddyAllocator allocator = new OffHeapBuddyAllocator(16 * 1024 * 1024, 64);

        long start = System.nanoTime();
        long address = allocator.allocate(64);
        long end = System.nanoTime();
        long duration = end - start;
        System.out.println("allocate() took " + duration + " ns"); //buddy: 3304600 slab: 17700

        start = System.nanoTime();
        allocator.free(address);
        end = System.nanoTime();
        duration = end - start;
        System.out.println("free() took " + duration + " ns");
    }
}
