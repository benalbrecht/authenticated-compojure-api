-- name: create-registered-user-table-if-not-exists!
-- create the registered_user table if it does not exist
CREATE TABLE IF NOT EXISTS registered_user (
   id              UUID PRIMARY KEY DEFAULT UUID_GENERATE_V4()
   , created_on    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   , email         CITEXT             NOT NULL UNIQUE
   , username      CITEXT             NOT NULL UNIQUE
   , password      TEXT               NOT NULL
   , refresh_token TEXT
);

-- name: drop-registered-user-table!
-- drop the registered_user table
DROP TABLE registered_user;
