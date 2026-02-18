package com.jabirhaque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class OffHeapSlabAllocatorTest {

    @Mock
    Unsafe unsafeMock;

    @Test
    public void testStandardAllocation() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(unsafeMock);

        List<Long> expected = new ArrayList<>();
        for (long i=100; i < 100 + 16*1024*1024; i+=64){
            expected.add(i);
        }
        Collections.reverse(expected);

        List<Long> actual = new ArrayList<>();
        while (true){
            try{
                actual.add(offHeapSlabAllocator.allocate(64));
            } catch (OutOfMemoryError e){
                break;
            }
        }
        Assertions.assertEquals(expected.size(), actual.size());
        for (int i=0; i<expected.size(); i++){
            Assertions.assertEquals(expected.get(i), actual.get(i));
        }
    }

    @Test
    public void testFIFOAllocationOrder() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(64)).thenReturn(0L);

        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(64, 16, unsafeMock);

        Assertions.assertEquals(48, offHeapSlabAllocator.allocate(16));
        Assertions.assertEquals(32, offHeapSlabAllocator.allocate(16));
        offHeapSlabAllocator.free(32);
        Assertions.assertEquals(32, offHeapSlabAllocator.allocate(16));
        Assertions.assertEquals(16, offHeapSlabAllocator.allocate(16));
        offHeapSlabAllocator.free(32);
        offHeapSlabAllocator.free(16);
        Assertions.assertEquals(16, offHeapSlabAllocator.allocate(16));
        Assertions.assertEquals(32, offHeapSlabAllocator.allocate(16));
        Assertions.assertEquals(0, offHeapSlabAllocator.allocate(16));
    }

    @Test
    public void blockSizeExceedsTotalSize(){

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new OffHeapSlabAllocator(512, 1024, unsafeMock);
        });

        Assertions.assertEquals("Block size cannot be greater than total size", exception.getMessage());
    }

    @Test
    public void outOfBlocksError() throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(32, 16, unsafeMock);
        offHeapSlabAllocator.allocate(16);
        offHeapSlabAllocator.allocate(16);

        Throwable exception = Assertions.assertThrows(OutOfMemoryError.class, () -> {
            offHeapSlabAllocator.allocate(16);
        });

        Assertions.assertEquals("Out of blocks", exception.getMessage());
    }

    @Test
    public void freeInvalidAddress() throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(32, 16, unsafeMock);
        offHeapSlabAllocator.allocate(16);
        offHeapSlabAllocator.allocate(16);

        offHeapSlabAllocator.free(16);
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            offHeapSlabAllocator.free(12);
        });

        Assertions.assertEquals("Provided address is invalid", exception.getMessage());
    }

    @Test
    public void requestExceedsBlockSize() throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(32, 16, unsafeMock);
        offHeapSlabAllocator.allocate(16);

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            offHeapSlabAllocator.allocate(24);
        });

        Assertions.assertEquals("Requested size exceeds block size", exception.getMessage());
    }

    @Test
    public void closeErrors() throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(32, 16, unsafeMock);
        offHeapSlabAllocator.close();

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            offHeapSlabAllocator.allocate(16);
        });
        Assertions.assertEquals("Allocator closed", exception.getMessage());

        exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            offHeapSlabAllocator.free(16);
        });
        Assertions.assertEquals("Allocator closed", exception.getMessage());
    }

    @Test
    public void closeWhilstAllocatedBlocks() throws NoSuchFieldException, IllegalAccessException {
        OffHeapSlabAllocator offHeapSlabAllocator = new OffHeapSlabAllocator(32, 16, unsafeMock);
        offHeapSlabAllocator.allocate(16);
        offHeapSlabAllocator.allocate(16);

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, offHeapSlabAllocator::close);
        Assertions.assertEquals("Cannot close allocator: 2 blocks still allocated", exception.getMessage());
    }
}
