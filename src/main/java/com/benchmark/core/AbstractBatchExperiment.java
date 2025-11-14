package com.benchmark.core;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractBatchExperiment {

    protected static final int COUNT = 10_000;
    protected static final int BATCH_SIZE = 1_000;

    protected double average(List<Long> times) {
        return times.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    protected void logBatchResults(
            String label,
            List<Double> insertAverages,
            List<Double> fetchAverages,
            List<Double> deleteAverages
    ) {
        for (int i = 0; i < insertAverages.size(); i++) {
            log.info(
                    "[{}] Batch {} - Insert Avg: {} ms, Fetch Avg: {} ms, Delete Avg: {} ms",
                    label,
                    i + 1,
                    insertAverages.get(i),
                    fetchAverages.get(i),
                    deleteAverages.get(i)
            );
        }
    }
}
