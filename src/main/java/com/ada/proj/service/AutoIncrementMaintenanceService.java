package com.ada.proj.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AutoIncrementMaintenanceService {

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final List<String> tables;
    private final boolean resequenceEnabled;
    private final String orderColumn;
    private final boolean resequencePrimaryEnabled;
    private final List<String> resequencePrimaryTables;
    private final AutoIncrementMaintenanceService self;

    public AutoIncrementMaintenanceService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.auto-increment.maintain.enabled:true}") boolean enabled,
        @Value("${app.auto-increment.maintain.tables:users}") String tablesCsv,
        @Value("${app.auto-increment.maintain.resequence-order:true}") boolean resequenceEnabled,
        @Value("${app.auto-increment.maintain.order-column:order_no}") String orderColumn,
        @Value("${app.auto-increment.maintain.resequence-primary.enabled:false}") boolean resequencePrimaryEnabled,
        @Value("${app.auto-increment.maintain.resequence-primary.tables:users}") String reseqPkTablesCsv,
        @Lazy AutoIncrementMaintenanceService self
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.resequenceEnabled = resequenceEnabled;
        this.orderColumn = orderColumn;
        this.tables = Arrays.stream(tablesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    this.resequencePrimaryEnabled = resequencePrimaryEnabled;
    this.resequencePrimaryTables = Arrays.stream(reseqPkTablesCsv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    this.self = self;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!enabled) return;
        safeMaintain("startup");
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        if (!enabled) return;
        safeMaintain("shutdown");
    }

    // 5분마다 실행 (고정 지연: 마지막 실행 끝난 후 5분)
    @Scheduled(fixedDelayString = "PT5M")
    public void periodic() {
        if (!enabled) return;
        safeMaintain("scheduled");
    }

    private void safeMaintain(String phase) {
        for (String table : tables) {
            try {
                maintainTable(table);
            } catch (Exception e) {
                log.warn("[AI-MAINTAIN] 단계={}, 테이블={} 실패: {}", phase, table, e.getMessage());
            }
        }
    }

    private void maintainTable(String table) {
    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
        long next = (count == null || count <= 0) ? 1 : count + 1;
        // MySQL AUTO_INCREMENT는 최소 1입니다. 0을 원하더라도 실제 저장은 1부터 시작합니다.
    jdbcTemplate.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = " + next);
    log.info("[AI-MAINTAIN] 테이블={}, 행수={}, AUTO_INCREMENT={}로 설정", table, count, next);
        
        // 2) 정렬 번호(order_column) 재계산 (옵션)
        if (resequenceEnabled) {
            try {
                ensureOrderColumnExists(table);
                resequenceOrderColumn(table);
            } catch (Exception e) {
                log.warn("[AI-RESEQUENCE] 테이블={} 실패: {}", table, e.getMessage());
            }
        }

        // 3) (옵션) PK 재시퀀싱: 매우 주의! FK를 모두 업데이트한 후 PK를 변경
        if (resequencePrimaryEnabled && resequencePrimaryTables.contains(table)) {
            try {
                // 트랜잭션 프록시를 통해 호출
                self.resequencePrimaryKey(table);
            } catch (Exception e) {
                log.warn("[AI-RESEQUENCE-PK] 테이블={} 실패: {}", table, e.getMessage());
            }
        }
    }

    private void ensureOrderColumnExists(String table) {
        // 호환성을 위해 IF NOT EXISTS 사용 대신 사전 존재 여부 확인 후 추가
        if (!columnExists(table, orderColumn)) {
            String sql = "ALTER TABLE `" + table + "` ADD COLUMN `" + orderColumn + "` BIGINT NOT NULL DEFAULT 0";
            jdbcTemplate.execute(sql);
        }
    }

    private void resequenceOrderColumn(String table) {
        String pk = getPrimaryKeyColumn(table);
        boolean hasCreatedAt = columnExists(table, "created_at");
        String orderBy = hasCreatedAt ? "`created_at`, `" + pk + "`" : "`" + pk + "`";

    // CTE 대신 서브쿼리 JOIN을 사용해 호환성 향상
    String sql = "UPDATE `" + table + "` t " +
        "JOIN (SELECT `" + pk + "` AS k, ROW_NUMBER() OVER (ORDER BY " + orderBy + ") AS rn FROM `" + table + "`) o " +
        "ON t.`" + pk + "` = o.k " +
        "SET t.`" + orderColumn + "` = o.rn";
    jdbcTemplate.execute(sql);
    log.info("[AI-RESEQUENCE] 테이블={} 컬럼={} 재정렬 완료, 기준={}", table, orderColumn, orderBy);
    }

    private String getPrimaryKeyColumn(String table) {
        String sql = "SELECT k.COLUMN_NAME" +
                " FROM information_schema.TABLE_CONSTRAINTS t" +
                " JOIN information_schema.KEY_COLUMN_USAGE k ON t.CONSTRAINT_NAME = k.CONSTRAINT_NAME" +
                " AND t.TABLE_SCHEMA = k.TABLE_SCHEMA AND t.TABLE_NAME = k.TABLE_NAME" +
                " WHERE t.CONSTRAINT_TYPE = 'PRIMARY KEY'" +
                " AND t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getString(1), table);
    }

    private boolean columnExists(String table, String column) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS" +
                " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, table, column);
        return n != null && n > 0;
    }

    @Transactional
    protected void resequencePrimaryKey(String table) {
        // SQL 템플릿 상수로 중복 문자열 경고 제거
        final String UPDATE_JOIN_REF_TEMPLATE = "UPDATE `%s` r JOIN `%s` m ON r.`%s` = m.old_pk SET r.`%s` = m.new_pk";
        final String UPDATE_JOIN_BASE_TEMPLATE = "UPDATE `%s` t JOIN `%s` m ON t.`%s` = m.old_pk SET t.`%s` = m.new_pk";
        final String UPDATE_MAP_ADD_OFFSET_TEMPLATE = "UPDATE `%s` SET new_pk = new_pk + %d";
        final String UPDATE_MAP_TO_FINAL_TEMPLATE = "UPDATE `%s` SET old_pk = new_pk, new_pk = new_pk - %d";

        String pk = getPrimaryKeyColumn(table);
        boolean hasCreatedAt = columnExists(table, "created_at");
        String orderBy = hasCreatedAt ? "`created_at`, `" + pk + "`" : "`" + pk + "`";

        final String map = "tmp_pk_map_" + table;
        final long offset = 1_000_000_000L;

        // 1) 매핑 테이블 준비 (TEMPORARY TABLE 사용)
        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS `" + map + "`");
        jdbcTemplate.execute("CREATE TEMPORARY TABLE `" + map + "` (old_pk BIGINT PRIMARY KEY, new_pk BIGINT NOT NULL)");
        jdbcTemplate.execute("INSERT INTO `" + map + "` (old_pk, new_pk) " +
                "SELECT `" + pk + "`, ROW_NUMBER() OVER (ORDER BY " + orderBy + ") FROM `" + table + "`");

        // 2) 중복 충돌 회피를 위해 큰 offset을 더한 값으로 1차 업데이트
    jdbcTemplate.execute(String.format(UPDATE_MAP_ADD_OFFSET_TEMPLATE, map, offset));

        // 2-1) FK 참조 업데이트 (참조 테이블/컬럼 탐색)
        for (FkRef fk : getReferencingFks(table, pk)) {
        jdbcTemplate.execute(String.format(UPDATE_JOIN_REF_TEMPLATE, fk.table, map, fk.column, fk.column));
        }

        // 2-2) 본 테이블 PK 업데이트 (offset 적용 값으로)
    jdbcTemplate.execute(String.format(UPDATE_JOIN_BASE_TEMPLATE, table, map, pk, pk));

        // 3) 최종 값으로 2차 업데이트 (offset 원복)
    jdbcTemplate.execute(String.format(UPDATE_MAP_TO_FINAL_TEMPLATE, map, offset));

        for (FkRef fk : getReferencingFks(table, pk)) {
            jdbcTemplate.execute(String.format(UPDATE_JOIN_REF_TEMPLATE, fk.table, map, fk.column, fk.column));
        }
        jdbcTemplate.execute(String.format(UPDATE_JOIN_BASE_TEMPLATE, table, map, pk, pk));

        log.info("[AI-RESEQUENCE-PK] 테이블={} 기본키 {} 재시퀀싱 완료 (시작=1)", table, pk);
    }

    private record FkRef(String table, String column) {}

    private List<FkRef> getReferencingFks(String referencedTable, String referencedColumn) {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = ? AND REFERENCED_COLUMN_NAME = ?";
        return jdbcTemplate.query(sql, (rs, rn) -> new FkRef(rs.getString(1), rs.getString(2)), referencedTable, referencedColumn);
    }
}
