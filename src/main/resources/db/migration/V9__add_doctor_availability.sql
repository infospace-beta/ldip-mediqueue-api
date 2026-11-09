ALTER TABLE users
    ADD COLUMN on_duty BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_on_duty_role
    CHECK (on_duty = FALSE OR role = 'DOCTOR');
