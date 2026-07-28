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

    @Override
    public String toString() {
        return "Allocation Statistics\n" +
                "----------------------\n" +
                "Allocations        : " + allocations + "\n" +
                "Frees              : " + frees + "\n" +
                "Active allocations : " + activeAllocations + "\n" +
                "Peak allocations   : " + peakAllocations + "\n" +
                "Failed allocations : " + failedAllocations + "\n" +
                "Bytes allocated    : " + byteAllocated + "\n" +
                "Peak bytes used    : " + peakBytesAllocated;
    }
}
