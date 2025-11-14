package com.benchmark.redis;

import com.benchmark.core.AbstractBatchExperiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("redis")
@RequiredArgsConstructor
public class RedisBatchExperiment extends AbstractBatchExperiment implements CommandLineRunner {

    private final RedisTemplate<String, Integer> redisTemplate;

    @Override
    public void run(String... args) {
        log.info("[Redis] Starting batch experiment (COUNT={}, BATCH_SIZE={})", COUNT, BATCH_SIZE);

        List<Double> batchInsertAverages = new ArrayList<>();
        List<Double> batchFetchAverages = new ArrayList<>();
        List<Double> batchDeleteAverages = new ArrayList<>();

        int totalBatches = COUNT / BATCH_SIZE;

        for (int batch = 0; batch < totalBatches; batch++) {
            log.info("[Redis] Running batch {}/{}", batch + 1, totalBatches);

            List<Long> insertTimes = performBatchOperations(batch, "INSERT");
            List<Long> fetchTimes  = performBatchOperations(batch, "SELECT");
            List<Long> deleteTimes = performBatchOperations(batch, "DELETE");

            batchInsertAverages.add(average(insertTimes));
            batchFetchAverages.add(average(fetchTimes));
            batchDeleteAverages.add(average(deleteTimes));

            log.info("[Redis] Finished batch {}/{}", batch + 1, totalBatches);
        }

        logBatchResults("Redis", batchInsertAverages, batchFetchAverages, batchDeleteAverages);
        log.info("[Redis] All batches completed.");
    }

    private List<Long> performBatchOperations(int batch, String operationType) {
        List<Long> operationTimes = new ArrayList<>();

        for (int i = 1; i <= BATCH_SIZE; i++) {
            int keyIndex = batch * BATCH_SIZE + i;
            String key = "user:" + keyIndex;

            switch (operationType) {
                case "INSERT":
                    operationTimes.add(executeRedisCommand(
                            () -> redisTemplate.opsForValue().set(key, keyIndex)
                    ));
                    break;

                case "SELECT":
                    operationTimes.add(executeRedisCommand(
                            () -> redisTemplate.opsForValue().get(key)
                    ));
                    break;

                case "DELETE":
                    operationTimes.add(executeRedisCommand(
                            () -> redisTemplate.delete(key)
                    ));
                    break;

                default:
                    throw new IllegalArgumentException("Unknown operationType: " + operationType);
            }
        }
        return operationTimes;
    }

    private long executeRedisCommand(Runnable command) {
        long startTime = System.currentTimeMillis();
        command.run();
        long duration = System.currentTimeMillis() - startTime;
        log.debug("[Redis] Executed command in {} ms", duration);
        return duration;
    }
}
