package com.jabirhaque;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AllocationStatistics {
    private long allocations;
    private long frees;
    private long activeAllocations;
    private long peakAllocations;
    private long failedAllocations;
    private long byteAllocated;
    private long peakBytesAllocated;
}
