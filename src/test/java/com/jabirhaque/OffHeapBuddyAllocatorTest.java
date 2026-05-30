package com.jabirhaque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class OffHeapBuddyAllocatorTest {

    @Mock
    Unsafe unsafeMock;

    @Test
    public void testStandardAllocation() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(unsafeMock);

        List<Long> expected = new ArrayList<>();
        for (long i = 100; i < 100 + 16 * 1024 * 1024; i += 64) {
            expected.add(i);
        }

        List<Long> actual = new ArrayList<>();
        while (true) {
            try {
                actual.add(offHeapBuddyAllocator.allocate(64));
            } catch (OutOfMemoryError e) {
                break;
            }
        }
        Assertions.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Assertions.assertEquals(expected.get(i), actual.get(i));
        }
    }

    @Test
    public void testOrderMaintained(){
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(unsafeMock);

        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));
        Assertions.assertEquals(100 + 8 * 1024 * 1024, offHeapBuddyAllocator.allocate(8 * 1024 * 1024));
        Assertions.assertEquals(100 + 4 * 1024 * 1024, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));

        offHeapBuddyAllocator.free(100);
        offHeapBuddyAllocator.free(100 + 4 * 1024 * 1024);

        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(8 * 1024 * 1024));

        offHeapBuddyAllocator.free(100);
        offHeapBuddyAllocator.free(100 + 8 * 1024 * 1024);

        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(16 * 1024 * 1024));
        offHeapBuddyAllocator.free(100);

        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));
        Assertions.assertEquals(100 + 4 * 1024 * 1024, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));
        Assertions.assertEquals(100 + 8 * 1024 * 1024, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));
        Assertions.assertEquals(100 + 12 * 1024 * 1024, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));

        offHeapBuddyAllocator.free(100);
        offHeapBuddyAllocator.free(100 + 8 * 1024 * 1024);

        Assertions.assertEquals(100 + 8 * 1024 * 1024, offHeapBuddyAllocator.allocate(4 * 1024 * 1024));
    }

    @Test
    public void blockSizeExceedsTotalSize(){

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new OffHeapBuddyAllocator(512, 1024, unsafeMock);
        });

        Assertions.assertEquals("Minimum block size cannot be greater than the total size", exception.getMessage());
    }

    @Test
    public void outOfBlocksError() throws NoSuchFieldException, IllegalAccessException {
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 4, unsafeMock);
        offHeapBuddyAllocator.allocate(16);
        offHeapBuddyAllocator.allocate(8);
        offHeapBuddyAllocator.allocate(4);
        offHeapBuddyAllocator.allocate(4);

        Throwable exception = Assertions.assertThrows(OutOfMemoryError.class, () -> {
            offHeapBuddyAllocator.allocate(4);
        });

        Assertions.assertEquals("Out of blocks to fit this request", exception.getMessage());
    }

    @Test
    public void smallerThanMinBlock() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(32)).thenReturn(100L);
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 4, unsafeMock);
        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(2));
    }

    @Test
    public void nonPowerOfTwo(){
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new OffHeapBuddyAllocator(13, 2, unsafeMock);
        });

        Assertions.assertEquals("Both total size and minimum block size must be powers of two", exception.getMessage());

        Throwable exception2 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new OffHeapBuddyAllocator(16, 3, unsafeMock);
        });

        Assertions.assertEquals("Both total size and minimum block size must be powers of two", exception2.getMessage());

        Throwable exception3 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new OffHeapBuddyAllocator(13, 3, unsafeMock);
        });

        Assertions.assertEquals("Both total size and minimum block size must be powers of two", exception3.getMessage());
    }
}