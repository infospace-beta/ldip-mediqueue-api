CREATE INDEX idx_emergency_cases_open_hospital_created
    ON emergency_cases (hospital_id, created_at DESC)
    WHERE status IN ('REPORTED', 'ASSIGNED', 'RESPONDING');
