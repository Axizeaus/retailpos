
-- Add a display name.
-- Existing accounts receive a neutral placeholder.
ALTER TABLE users
    ADD COLUMN name VARCHAR(150);

UPDATE users
SET name = 'Existing user';

ALTER TABLE users
    ALTER COLUMN name SET NOT NULL;

-- Email is now optional.
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

-- Add an optional phone number.
ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(16);

-- Every account must have at least one login identifier.
ALTER TABLE users
    ADD CONSTRAINT chk_users_login_identifier
        CHECK (email IS NOT NULL OR phone_number IS NOT NULL);

-- Phone numbers are stored in normalized E.164 format.
-- Multiple NULL values are allowed.
CREATE UNIQUE INDEX uk_users_phone_number
    ON users (phone_number)
    WHERE phone_number IS NOT NULL;
