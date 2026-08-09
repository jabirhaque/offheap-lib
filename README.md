# offheap-lib

This project is a simple **Java Off Heap Memory Management Library** ideal for low latency, memory intensive applications.

## Features
- Slab allocator for high-performance fixed-size memory allocations with safe allocation and deallocation handling
- Buddy allocator for power-of-two-size memory allocations with O(log(n)) block splitting and merging to minimise internal fragmentation
- Free list allocator for variable-size memory allocations with O(1) block splitting and merging to eliminate internal fragmentation at the expense of external fragmentation
- Concurrent slab, buddy and free list allocators with sharded allocation pools to improve scalability and reduce contention in multithreaded workloads
- JMH benchmarking on partially fragmented 16 MB allocator instances, with slab allocator achieving 45 ns allocations and 70 ns frees, buddy allocator achieving 90 ns allocations and 65 ns frees and free list allocator achieving 43 ns allocation and 41 ns free  

| Benchmark        | Mode | Cnt | Score (ns/op) | Error (ns/op) |
|------------------|------|-----|---------------|---------------|
| slabAllocate     | avgt | 25  | 44.790        | ± 0.096       |
| slabFree         | avgt | 25  | 70.021        | ± 0.141       |
| buddyAllocate    | avgt | 25  | 90.465        | ± 4.167       |
| buddyFree        | avgt | 25  | 65.382        | ± 4.497       |
| freeListAllocate | avgt | 25  | 42.934        | ± 4.196       |
| freeListFree     | avgt | 25  | 41.443        | ± 3.110       |

## Technology Stack
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)