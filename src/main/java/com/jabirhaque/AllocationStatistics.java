package com.jabirhaque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class AllocationStatistics {
    private final long totalBytes;
    private long allocations;
    private long frees;
    private long activeAllocations;
    private long failedAllocations;
    private long bytesAllocated;

    AllocationStatistics(long totalBytes){
        this.totalBytes = totalBytes;
    }

    @Override
    public String toString() {
        return "Allocation Statistics\n" +
                "----------------------\n" +
                "Total Bytes        : " + totalBytes + "\n" +
                "Allocations        : " + allocations + "\n" +
                "Frees              : " + frees + "\n" +
                "Active allocations : " + activeAllocations + "\n" +
                "Failed allocations : " + failedAllocations + "\n" +
                "Bytes allocated    : " + bytesAllocated + "\n";
    }
}
