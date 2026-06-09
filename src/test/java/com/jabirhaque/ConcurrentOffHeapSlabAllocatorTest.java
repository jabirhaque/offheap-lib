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
public class ConcurrentOffHeapSlabAllocatorTest {
    @Mock
    Unsafe unsafeMock;

    @Test
    public void testStandardAllocationForOneAllocator() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapSlabAllocator concurrentOffHeapSlabAllocator = new ConcurrentOffHeapSlabAllocator(16 * 1024 * 1024, 64, 1, unsafeMock);

        List<Long> expected = new ArrayList<>();
        for (long i=100; i < 100 + 16*1024*1024; i+=64){
            expected.add(i);
        }
        Collections.reverse(expected);

        List<Long> actual = new ArrayList<>();
        while (true){
            try{
                actual.add(concurrentOffHeapSlabAllocator.allocate(64));
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
    public void testStandardAllocationCountForMultipleAllocators() throws NoSuchFieldException, IllegalAccessException {
        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapSlabAllocator concurrentOffHeapSlabAllocator = new ConcurrentOffHeapSlabAllocator(16 * 1024 * 1024, 64, 4, unsafeMock);

        Set<Long> uniqueAllocations = new HashSet<>();
        for (int i=0; i<1024; i++){
            uniqueAllocations.add(concurrentOffHeapSlabAllocator.allocate(64));
        }
        Assertions.assertEquals(1024, uniqueAllocations.size());
    }

    @Test
    void concurrentAllocateShouldReturnDifferentAddresses() throws NoSuchFieldException, IllegalAccessException, InterruptedException {

        Mockito.when(unsafeMock.allocateMemory(16 * 1024 * 1024)).thenReturn(100L);

        ConcurrentOffHeapSlabAllocator concurrentOffHeapSlabAllocator = new ConcurrentOffHeapSlabAllocator(16 * 1024 * 1024, 64, 4, unsafeMock);

        Set<Long> set = ConcurrentHashMap.newKeySet();

        List<Thread> threads = new ArrayList<>();

        for (int i=0; i<8; i++){
            threads.add(
                    new Thread(() -> {
                        for (int j=0; j<128; j++){
                            set.add(concurrentOffHeapSlabAllocator.allocate(64));
                        }
                    })
            );
        }

        for (Thread thread: threads) thread.start();
        for (Thread thread: threads) thread.join();

        Assertions.assertEquals(8*128, set.size());
    }
}
