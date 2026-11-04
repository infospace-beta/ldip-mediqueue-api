ALTER TABLE users
    ADD COLUMN hospital_id BIGINT;

ALTER TABLE users
    ADD CONSTRAINT fk_users_hospital
    FOREIGN KEY (hospital_id)
    REFERENCES hospitals(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_users_hospital_id
    ON users (hospital_id);
