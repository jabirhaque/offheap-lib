package com.jabirhaque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sun.misc.Unsafe;

import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
public class ConcurrentOffHeapBuddyAllocatorTest {
    @Mock
    Unsafe unsafeMock;

    @Test
    public void testStandardAllocation() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16 * 1024 * 1024, 64, 1, unsafeMock);

        List<Long> expected = new ArrayList<>();
        for (long i = 100; i < 100 + 16 * 1024 * 1024; i += 64) {
            expected.add(i);
        }

        List<Long> actual = new ArrayList<>();
        while (true) {
            try {
                actual.add(concurrentOffHeapBuddyAllocator.allocate(64));
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
    public void testStandardAllocationCountForMultipleAllocators() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16 * 1024 * 1024, 64, 4, unsafeMock);

        Set<Long> uniqueAllocations = new HashSet<>();
        for (int i = 0; i < 1024; i++) {
            uniqueAllocations.add(concurrentOffHeapBuddyAllocator.allocate(64));
        }
        Assertions.assertEquals(1024, uniqueAllocations.size());
    }

    @Test
    void concurrentAllocateShouldReturnDifferentAddresses() throws NoSuchFieldException, IllegalAccessException, InterruptedException {

        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16 * 1024 * 1024, 64, 4, unsafeMock);

        Set<Long> set = ConcurrentHashMap.newKeySet();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            threads.add(
                    new Thread(() -> {
                        for (int j = 0; j < 128; j++) {
                            set.add(concurrentOffHeapBuddyAllocator.allocate(64));
                        }
                    })
            );
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        Assertions.assertEquals(8 * 128, set.size());
    }

    @Test
    public void throwsOnBlockSize() throws NoSuchFieldException, IllegalAccessException {

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16, 8, 4, unsafeMock);
        });

        Assertions.assertEquals("Failed to initiate allocators", exception.getMessage());
    }

    @Test
    public void throwsOnTotalSizeOrAllocatorCount() {
        Throwable firstException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(8, 8, 16, unsafeMock);
        });
        Assertions.assertEquals("Total size must be greater than allocator count", firstException.getMessage());

        Throwable secondException = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16, 8, -1, unsafeMock);
        });
        Assertions.assertEquals("Allocator count must be at least one", secondException.getMessage());
    }

    @Test
    public void freeTest() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(64)).thenReturn(0L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(64, 16, 1, unsafeMock);

        long address = concurrentOffHeapBuddyAllocator.allocate(16);

        concurrentOffHeapBuddyAllocator.free(address);
    }

    @Test
    public void testFreeAndClose() throws Exception {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16 * 1024 * 1024, 64, 4, unsafeMock);

        Set<Long> set = ConcurrentHashMap.newKeySet();

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            threads.add(
                    new Thread(() -> {
                        for (int j = 0; j < 128; j++) {
                            set.add(concurrentOffHeapBuddyAllocator.allocate(64));
                        }
                    })
            );
        }

        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        Assertions.assertEquals(8 * 128, set.size());

        for (long address : set) concurrentOffHeapBuddyAllocator.free(address);

        concurrentOffHeapBuddyAllocator.close();

        Throwable exception = Assertions.assertThrows(IllegalStateException.class, () -> concurrentOffHeapBuddyAllocator.allocate(64));
        Assertions.assertEquals("Allocator closed", exception.getMessage());
    }

    @Test
    public void testInvalidFree() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16)).thenReturn(0L);

        ConcurrentOffHeapBuddyAllocator concurrentOffHeapBuddyAllocator = new ConcurrentOffHeapBuddyAllocator(16, 4, 2, unsafeMock);

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> concurrentOffHeapBuddyAllocator.free(3));
        Assertions.assertEquals("Provided address is invalid", exception.getMessage());
    }
}