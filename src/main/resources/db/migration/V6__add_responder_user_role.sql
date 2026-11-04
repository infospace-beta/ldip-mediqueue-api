ALTER TABLE users
    DROP CONSTRAINT chk_users_role;

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
    CHECK (role IN (
        'PATIENT',
        'DOCTOR',
        'RECEPTIONIST',
        'MEDICAL_STAFF',
        'RESPONDER',
        'HOSPITAL_ADMIN',
        'SYSTEM_ADMIN'
    ));
