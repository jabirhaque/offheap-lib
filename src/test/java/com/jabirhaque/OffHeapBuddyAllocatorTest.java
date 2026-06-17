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

        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(16 * 1024 * 1024, 64, unsafeMock);

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

        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(16 * 1024 * 1024, 64, unsafeMock);

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

    @Test
    public void freeInvalidAddres(){
        Mockito.when(unsafeMock.allocateMemory(32)).thenReturn(100L);
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 4, unsafeMock);

        Assertions.assertEquals(100, offHeapBuddyAllocator.allocate(16));
        Assertions.assertEquals(116, offHeapBuddyAllocator.allocate(16));

        offHeapBuddyAllocator.free(100);

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            offHeapBuddyAllocator.free(99);
        });

        offHeapBuddyAllocator.free(116);

        Assertions.assertEquals("Provided address is invalid", exception.getMessage());
    }

    @Test
    public void requestExceedsTotalSize(){
        Mockito.when(unsafeMock.allocateMemory(32)).thenReturn(100L);
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 4, unsafeMock);

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            offHeapBuddyAllocator.allocate(33);
        });

        Assertions.assertEquals("Requested size exceeds total size", exception.getMessage());
    }

    @Test
    public void closeErrors() throws NoSuchFieldException, IllegalAccessException {
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 16, unsafeMock);
        offHeapBuddyAllocator.close();

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            offHeapBuddyAllocator.allocate(16);
        });
        Assertions.assertEquals("Allocator closed", exception.getMessage());

        exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            offHeapBuddyAllocator.free(16);
        });
        Assertions.assertEquals("Allocator closed", exception.getMessage());
    }

    @Test
    public void closeWhilstAllocatedBlocks() throws NoSuchFieldException, IllegalAccessException {
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(32, 16, unsafeMock);
        offHeapBuddyAllocator.allocate(16);
        offHeapBuddyAllocator.allocate(16);

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, offHeapBuddyAllocator::close);
        Assertions.assertEquals("Cannot close allocator, blocks still allocated", exception.getMessage());
    }

    @Test
    public void throwsOnBlockLimit() throws NoSuchFieldException, IllegalAccessException {

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator((long)Integer.MAX_VALUE+1, 1, unsafeMock);
        });
        Assertions.assertEquals("Block count exceeds limit", exception.getMessage());
    }

    @Test
    public void zeroBlockSize() throws NoSuchFieldException, IllegalAccessException {

        Throwable zeroBlockException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(128, 0, unsafeMock);
        });
        Assertions.assertEquals("Total and block size must be at least one byte", zeroBlockException.getMessage());

        Throwable negativeBlockException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(128, -1, unsafeMock);
        });
        Assertions.assertEquals("Total and block size must be at least one byte", negativeBlockException.getMessage());

        Throwable zeroTotalSizeException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(0, -1, unsafeMock);
        });
        Assertions.assertEquals("Total and block size must be at least one byte", zeroTotalSizeException.getMessage());

        Throwable negativeTotalSizeException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(-1, -1, unsafeMock);
        });
        Assertions.assertEquals("Total and block size must be at least one byte", negativeTotalSizeException.getMessage());
    }

    @Test
    public void readWriteIntTest(){
        Mockito.when(unsafeMock.allocateMemory(128)).thenReturn(0L);
        Mockito.when(unsafeMock.getInt(0)).thenReturn(1);
        Mockito.when(unsafeMock.getInt(4)).thenReturn(2);
        OffHeapBuddyAllocator offHeapBuddyAllocator = new OffHeapBuddyAllocator(128, 16, unsafeMock);
        Assertions.assertEquals(0, offHeapBuddyAllocator.allocate(16));
        offHeapBuddyAllocator.writeInt(0, 0, 1);
        offHeapBuddyAllocator.writeInt(0, 4, 2);
        Assertions.assertEquals(1, offHeapBuddyAllocator.readInt(0, 0));
        Assertions.assertEquals(2, offHeapBuddyAllocator.readInt(0, 4));
    }
}