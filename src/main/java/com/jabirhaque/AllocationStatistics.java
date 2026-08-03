package com.jabirhaque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class AllocationStatistics {
    private long allocations;
    private long frees;
    private long activeAllocations;
    private long failedAllocations;
    private long byteAllocated;

    @Override
    public String toString() {
        return "Allocation Statistics\n" +
                "----------------------\n" +
                "Allocations        : " + allocations + "\n" +
                "Frees              : " + frees + "\n" +
                "Active allocations : " + activeAllocations + "\n" +
                "Failed allocations : " + failedAllocations + "\n" +
                "Bytes allocated    : " + byteAllocated + "\n";
    }
}
