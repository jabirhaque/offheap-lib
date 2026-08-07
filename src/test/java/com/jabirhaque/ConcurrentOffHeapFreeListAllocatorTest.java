
package com.jabirhaque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentOffHeapFreeListAllocatorTest {

    private ConcurrentOffHeapFreeListAllocator allocator;

    @BeforeEach
    void setUp() throws Exception {
        allocator = new ConcurrentOffHeapFreeListAllocator(4*8192, 16, 4);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (allocator != null) {
            AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();
            if (stats.getActiveAllocations() == 0) {
                allocator.close();
            }
        }
    }

    @Test
    void shouldAllocateMemorySuccessfully() {
        long address = allocator.allocate(64);

        assertTrue(address > 0);

        AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();
        assertEquals(1, stats.getAllocations());
        assertEquals(1, stats.getActiveAllocations());
    }

    @Test
    void shouldRejectZeroByteAllocation() {
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(0));
    }

    @Test
    void shouldRejectNegativeAllocation() {
        assertThrows(IllegalArgumentException.class,
                () -> allocator.allocate(-1));
    }

    @Test
    void shouldAllocateMultipleBlocks() {
        long a = allocator.allocate(64);
        long b = allocator.allocate(64);

        assertNotEquals(a, b);

        AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();
        assertEquals(2, stats.getActiveAllocations());
    }

    @Test
    void shouldFreeMemorySuccessfully() {
        long address = allocator.allocate(64);

        allocator.free(address);

        AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();

        assertEquals(0, stats.getActiveAllocations());
        assertEquals(1, stats.getFrees());
    }

    @Test
    void shouldRejectDoubleFree() {
        long address = allocator.allocate(64);

        allocator.free(address);

        assertThrows(IllegalArgumentException.class,
                () -> allocator.free(address));
    }

    @Test
    void shouldRejectInvalidFreeAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> allocator.free(12345));
    }

    @Test
    void shouldWriteAndReadInt() {
        long address = allocator.allocate(Integer.BYTES);

        allocator.writeInt(address, 0, 12345);

        assertEquals(12345,
                allocator.readInt(address, 0));
    }

    @Test
    void shouldRejectOutOfBoundsWrite() {
        long address = allocator.allocate(4);

        assertThrows(IllegalArgumentException.class,
                () -> allocator.writeInt(address, 4, 5));
    }

    @Test
    void shouldRejectOutOfBoundsRead() {
        long address = allocator.allocate(4);

        assertThrows(IllegalArgumentException.class,
                () -> allocator.readInt(address, 4));
    }

    @Test
    void shouldRejectReadAfterFree() {
        long address = allocator.allocate(4);

        allocator.free(address);

        assertThrows(IllegalArgumentException.class,
                () -> allocator.readInt(address, 0));
    }

    @Test
    void shouldNotCloseWithOutstandingAllocations() {
        allocator.allocate(64);

        assertThrows(IllegalStateException.class,
                () -> allocator.close());
    }

    @Test
    void shouldCloseSuccessfullyWhenEmpty() throws Exception {
        allocator.close();

        assertThrows(IllegalStateException.class,
                () -> allocator.allocate(16));
    }

    @Test
    void shouldTrackStatisticsCorrectly() {
        long a = allocator.allocate(32);
        long b = allocator.allocate(32);

        allocator.free(a);

        AllocationStatistics stats = allocator.getAllocationStatisticsSnapshot();

        assertEquals(2, stats.getAllocations());
        assertEquals(1, stats.getFrees());
        assertEquals(1, stats.getActiveAllocations());

        allocator.free(b);
    }

    @Test
    void shouldAllowConcurrentAllocations() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(8);

        Set<Long> addresses = ConcurrentHashMap.newKeySet();

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            futures.add(executor.submit(() -> {
                long addr = allocator.allocate(32);
                addresses.add(addr);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        assertEquals(100, addresses.size());

        executor.shutdown();
    }

    @Test
    void shouldAllowConcurrentAllocateWriteReadAndFree() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(8);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {

            final int value = i;

            futures.add(executor.submit(() -> {

                long address = allocator.allocate(32);

                allocator.writeInt(address, 0, value);

                assertEquals(value,
                        allocator.readInt(address, 0));

                allocator.free(address);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(0,
                stats.getActiveAllocations());

        assertEquals(stats.getAllocations(),
                stats.getFrees());

        executor.shutdown();
    }

    @Test
    void shouldAllowDifferentThreadToFreeAllocation() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        long address = allocator.allocate(64);

        Future<?> future = executor.submit(() -> allocator.free(address));

        future.get();

        assertEquals(0,
                allocator.getAllocationStatisticsSnapshot()
                        .getActiveAllocations());

        executor.shutdown();
    }

    @Test
    void shouldAllowDifferentThreadToReadAndWriteAllocation() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        long address = allocator.allocate(32);

        executor.submit(() ->
                        allocator.writeInt(address, 0, 999))
                .get();

        Future<Integer> future =
                executor.submit(() ->
                        allocator.readInt(address, 0));

        assertEquals(999, future.get());

        allocator.free(address);

        executor.shutdown();
    }

    @Test
    void shouldHandleConcurrentStatisticsRequests() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(8);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {

            futures.add(executor.submit(() -> {

                long addr = allocator.allocate(16);

                allocator.getAllocationStatisticsSnapshot();

                allocator.free(addr);

                allocator.getAllocationStatisticsSnapshot();
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(0,
                stats.getActiveAllocations());

        executor.shutdown();
    }

    @Test
    void shouldSurviveHighContention() throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(16);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 16; i++) {

            futures.add(executor.submit(() -> {

                for (int j = 0; j < 500; j++) {

                    long addr = allocator.allocate(32);

                    allocator.writeInt(addr, 0, j);

                    assertEquals(j,
                            allocator.readInt(addr, 0));

                    allocator.free(addr);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        AllocationStatistics stats =
                allocator.getAllocationStatisticsSnapshot();

        assertEquals(0, stats.getActiveAllocations());
        assertEquals(stats.getAllocations(),
                stats.getFrees());

        executor.shutdown();
    }
}