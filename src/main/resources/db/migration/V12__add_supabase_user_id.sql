ALTER TABLE users
    ADD COLUMN supabase_user_id UUID;

CREATE UNIQUE INDEX uk_users_supabase_user_id
    ON users (supabase_user_id);
