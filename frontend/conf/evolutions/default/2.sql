# --- !Ups

ALTER TABLE "users"
  ADD COLUMN "username" character varying(100) NOT NULL DEFAULT 'foo';
ALTER TABLE "users" ALTER COLUMN "username" DROP DEFAULT;

ALTER TABLE "users"
  ADD CONSTRAINT accounts_unique_username UNIQUE ("username");

COMMENT ON COLUMN "users"."username" IS 'The unique username of the user in the system.';

# --- !Downs