package com.jabirhaque;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OffHeapFreeListAllocatorTest {

    private OffHeapFreeListAllocator allocator;

    private static final long HEADER_SIZE = Byte.BYTES + 4*Long.BYTES;

    @BeforeEach
    void setup() throws Exception {
        allocator = new OffHeapFreeListAllocator(1024, 16);
    }

    @Test
    void testAllocationOrder() {
        List<Long> allocated = new ArrayList<>();
        while (true){
            try{
                allocated.add(allocator.allocate(64));
            } catch (OutOfMemoryError e){
                break;
            }
        }
        for (int i=1; i<allocated.size(); i++){
            assertEquals(allocated.get(i), allocated.get(i-1) + 64 + HEADER_SIZE);
        }
    }

    @Test
    void shouldAllocateMemorySuccessfully() {
        long address = allocator.allocate(64);

        assertTrue(address > 0);
        assertTrue(allocator.allocated());

        AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();

        assertEquals(1, stats.getAllocations());
        assertEquals(1, stats.getActiveAllocations());
        assertEquals(0, stats.getFrees());
    }

    @Test
    void shouldRejectZeroByteAllocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.allocate(0)
        );
    }

    @Test
    void shouldRejectNegativeAllocation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.allocate(-10)
        );
    }

    @Test
    void shouldAllocateMultipleBlocks() {
        long first = allocator.allocate(100);
        long second = allocator.allocate(100);

        assertNotEquals(first, second);

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(2, stats.getActiveAllocations());
    }

    @Test
    void shouldSplitLargeBlockWhenAllocatingSmallAmount() {
        long address = allocator.allocate(32);

        assertTrue(address > 0);

        long second = allocator.allocate(32);

        assertTrue(second > address);

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(2, stats.getActiveAllocations());
    }


    @Test
    void shouldFreeAllocatedBlock() {
        long address = allocator.allocate(64);

        allocator.free(address);

        assertFalse(allocator.allocated());

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(1, stats.getFrees());
        assertEquals(0, stats.getActiveAllocations());
    }


    @Test
    void shouldRejectDoubleFree() {
        long address = allocator.allocate(64);

        allocator.free(address);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.free(address)
        );
    }


    @Test
    void shouldRejectInvalidAddressOnFree() {
        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.free(12345)
        );
    }


    @Test
    void shouldReuseFreedMemory() {
        long first = allocator.allocate(64);

        allocator.free(first);

        long second = allocator.allocate(64);

        assertEquals(first, second);
    }


    @Test
    void shouldMergeWithNextFreeBlock() {
        long first = allocator.allocate(64);
        long second = allocator.allocate(64);

        allocator.free(second);
        allocator.free(first);

        long reused = allocator.allocate(120);

        assertEquals(first, reused);
    }


    @Test
    void shouldMergeWithPreviousFreeBlock() {
        long first = allocator.allocate(64);
        long second = allocator.allocate(64);

        allocator.free(first);
        allocator.free(second);

        long reused = allocator.allocate(120);

        assertEquals(first, reused);
    }


    @Test
    void shouldFailWhenMemoryIsExhausted() throws Exception {

        OffHeapFreeListAllocator small =
                new OffHeapFreeListAllocator(128, 16);

        long addr = small.allocate(80);

        assertThrows(
                OutOfMemoryError.class,
                () -> small.allocate(80)
        );

        small.free(addr);

        small.close();
    }


    @Test
    void shouldWriteAndReadIntSuccessfully() {
        long address = allocator.allocate(Integer.BYTES);

        allocator.writeInt(address, 0, 12345);

        int value = allocator.readInt(address, 0);

        assertEquals(12345, value);
    }


    @Test
    void shouldRejectWriteOutsideAllocatedRange() {
        long address = allocator.allocate(4);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.writeInt(address, 4, 10)
        );
    }


    @Test
    void shouldRejectReadOutsideAllocatedRange() {
        long address = allocator.allocate(4);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.readInt(address, 4)
        );
    }


    @Test
    void shouldNotAllowAccessAfterFree() {
        long address = allocator.allocate(8);

        allocator.free(address);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.readInt(address, 0)
        );
    }


    @Test
    void shouldCloseAllocatorSuccessfullyWhenEmpty() {
        allocator.close();

        assertThrows(
                IllegalStateException.class,
                () -> allocator.allocate(10)
        );
    }


    @Test
    void shouldNotCloseWhenBlocksAreAllocated() {
        allocator.allocate(64);

        assertThrows(
                IllegalStateException.class,
                () -> allocator.close()
        );
    }


    @Test
    void shouldAllowMultipleCloseCalls() {
        allocator.close();

        assertDoesNotThrow(
                () -> allocator.close()
        );
    }


    @Test
    void shouldTrackFailedAllocations() {
        try {
            allocator.allocate(-1);
        } catch (IllegalArgumentException ignored) {
        }

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(
                1,
                stats.getFailedAllocations()
        );
    }


    @Test
    void shouldTrackAllocatedBytes() {
        long address = allocator.allocate(100);

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertTrue(stats.getBytesAllocated() > 100);

        allocator.free(address);

        stats = allocator.getAllocationStatisticsSnapshot();

        assertEquals(
                0,
                stats.getBytesAllocated()
        );
    }
}