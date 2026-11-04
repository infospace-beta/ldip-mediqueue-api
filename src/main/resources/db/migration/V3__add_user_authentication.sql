-- Add authentication fields to existing users.
-- Existing user records are preserved.

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'PATIENT',
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;

-- Prevent invalid role values.
ALTER TABLE users
    ADD CONSTRAINT chk_users_role
    CHECK (role IN (
        'PATIENT',
        'DOCTOR',
        'RECEPTIONIST',
        'MEDICAL_STAFF',
        'HOSPITAL_ADMIN',
        'SYSTEM_ADMIN'
    ));