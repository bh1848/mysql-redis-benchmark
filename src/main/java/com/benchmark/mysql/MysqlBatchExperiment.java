package com.benchmark.mysql;

import com.benchmark.core.AbstractBatchExperiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("mysql")
@RequiredArgsConstructor
public class MysqlBatchExperiment extends AbstractBatchExperiment implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("[MySQL] Starting batch experiment (COUNT={}, BATCH_SIZE={})", COUNT, BATCH_SIZE);

        List<Double> batchInsertAverages = new ArrayList<>();
        List<Double> batchFetchAverages  = new ArrayList<>();
        List<Double> batchDeleteAverages = new ArrayList<>();

        int totalBatches = COUNT / BATCH_SIZE;

        for (int batch = 0; batch < totalBatches; batch++) {
            log.info("[MySQL] Running batch {}/{}", batch + 1, totalBatches);

            List<Long> insertTimes = performBatchOperations(batch, "INSERT");
            List<Long> fetchTimes  = performBatchOperations(batch, "SELECT");
            List<Long> deleteTimes = performBatchOperations(batch, "DELETE");

            batchInsertAverages.add(average(insertTimes));
            batchFetchAverages.add(average(fetchTimes));
            batchDeleteAverages.add(average(deleteTimes));

            log.info("[MySQL] Finished batch {}/{}", batch + 1, totalBatches);
        }

        logBatchResults("MySQL", batchInsertAverages, batchFetchAverages, batchDeleteAverages);
        log.info("[MySQL] All batches completed.");
    }

    private List<Long> performBatchOperations(int batch, String operationType) {
        List<Long> operationTimes = new ArrayList<>();
        String sql;

        for (int i = 1; i <= BATCH_SIZE; i++) {
            int keyIndex = batch * BATCH_SIZE + i;

            switch (operationType) {
                case "INSERT":
                    sql = "INSERT INTO test_data (id, name, key_index) VALUES (?, ?, ?)";
                    operationTimes.add(executeUpdateSql(sql, keyIndex, "TestData" + keyIndex, keyIndex));
                    break;

                case "SELECT":
                    sql = "SELECT id FROM test_data WHERE id = ?";
                    operationTimes.add(executeQuerySql(sql, keyIndex));
                    break;

                case "DELETE":
                    sql = "DELETE FROM test_data WHERE id = ?";
                    operationTimes.add(executeUpdateSql(sql, keyIndex));
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported operationType: " + operationType);
            }
        }

        return operationTimes;
    }

    private long executeUpdateSql(String sql, Object... params) {
        long startTime = System.currentTimeMillis();
        jdbcTemplate.update(sql, params);
        long duration = System.currentTimeMillis() - startTime;
        log.debug("[MySQL] Executed '{}' with params {} in {} ms", sql, params, duration);
        return duration;
    }

    private long executeQuerySql(String sql, Object param) {
        long startTime = System.currentTimeMillis();
        jdbcTemplate.queryForObject(sql, new Object[]{param}, Integer.class);
        long duration = System.currentTimeMillis() - startTime;
        log.debug("[MySQL] Executed '{}' with param {} in {} ms", sql, param, duration);
        return duration;
    }
}
