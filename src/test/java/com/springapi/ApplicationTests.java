package com.springapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Checks that the whole application starts. Needs Docker; skipped automatically when Docker is unavailable.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void allVersionedMigrationsAreApplied() {
		Integer appliedMigrations = jdbcTemplate.queryForObject(
				"""
				select count(*)
				from flyway_schema_history
				where success = true
				  and version in (
				      '1', '2', '3', '4', '5', '6', '7', '8', '9', '10'
				  )
				""",
				Integer.class);

		assertEquals(10, appliedMigrations);
	}

	@Test
	void queueAndEmergencyLookupIndexesExist() {
		Set<String> indexes = jdbcTemplate.queryForList("""
				select indexname
				from pg_indexes
				where schemaname = current_schema()
				  and tablename in ('queue_tickets', 'emergency_cases')
				""", String.class).stream().collect(Collectors.toSet());

		assertTrue(indexes.contains(
				"idx_queue_tickets_department_status_created"));
		assertTrue(indexes.contains("idx_queue_tickets_patient_created"));
		assertTrue(indexes.contains(
				"uk_queue_tickets_active_patient_department"));
		assertTrue(indexes.contains(
				"idx_emergency_cases_hospital_status_created"));
		assertTrue(indexes.contains(
				"idx_emergency_cases_open_hospital_created"));
	}

}
