package com.jabirhaque;

public class Main {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(16 * 1024 * 1024, 64);
        long address = offHeapSlabAllocator.allocate(64);
        System.out.println("Block Address: " + address);
        for (int i=0; i<64; i+=4){
            offHeapSlabAllocator.writeInt(address, i, i/4);
        }
        for (int i=0; i<64; i+=4){
            System.out.println(offHeapSlabAllocator.readInt(address, i));
        }

        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(16 * 1024 * 1024, 64);
        address = offHeapBuddyAllocator.allocate(64);
        System.out.println("Block Address: " + address);
        for (int i=0; i<64; i+=4){
            offHeapBuddyAllocator.writeInt(address, i, i/4);
        }
        for (int i=0; i<64; i+=4){
            System.out.println(offHeapBuddyAllocator.readInt(address, i));
        }
    }
}
