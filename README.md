# offheap-lib

This project is a simple **Java Off Heap Memory Management Library** ideal for low latency, memory intensive applications.

## Features
- Slab allocator for high-performance fixed-size memory allocations with safe allocation and deallocation handling
- Buddy allocator for variable-size memory allocation with efficient block splitting and merging to minimise fragmentation
- Concurrent slab allocator with sharded allocation pools to improve scalability and reduce contention in multi-threaded fixed-size workloads
- Concurrent buddy allocator supporting thread-safe variable-size allocations with independent allocator instances for improved parallel performance
- JMH benchmarking with slab allocator achieving 30 ns allocations and 40 ns frees, while buddy allocator provides power of two allocation with microsecond scale allocation latency


## Technology Stack
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)