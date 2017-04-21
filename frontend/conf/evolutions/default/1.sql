# --- !Ups

CREATE TABLE "users"
(
  "user_id" uuid NOT NULL,
  "provider_id" character varying(254) DEFAULT NULL,
  "provider_key" character varying(254) DEFAULT NULL,
  "email" character varying(128) DEFAULT NULL,
  "firstname" character varying(60) DEFAULT NULL,
  "lastname" character varying(80) DEFAULT NULL,
  "hasher" character varying DEFAULT NULL,
  "password" character varying DEFAULT NULL,
  "salt" character varying DEFAULT NULL,
  "oauth1_token" character varying DEFAULT NULL,
  "oauth1_secret" character varying DEFAULT NULL,
  "oauth2_access_token" character varying DEFAULT NULL,
  "oauth2_token_type" character varying DEFAULT NULL,
  "oauth2_expires" int,
  "oauth2_refresh_token" character varying DEFAULT NULL,
  "oauth2_params" text DEFAULT NULL,
  "avatar_url" character varying DEFAULT NULL,
  "activated" boolean DEFAULT false,
  "active" boolean DEFAULT true,
  "created_at" timestamp with time zone DEFAULT NULL,
  "updated_at" timestamp with time zone DEFAULT NULL,
  "admin" boolean DEFAULT false,
  "moderator" boolean DEFAULT false,
  CONSTRAINT accounts_pk PRIMARY KEY ("user_id"),
  CONSTRAINT accounts_unique_email UNIQUE ("email")
)
WITH (
  OIDS=FALSE
);
COMMENT ON TABLE "users" IS 'This table holds the user accounts for the system.';
COMMENT ON COLUMN "users"."user_id" IS 'The id of the account.';
COMMENT ON COLUMN "users"."provider_id" IS 'Provider ID of the LoginInfo.';
COMMENT ON COLUMN "users"."provider_key" IS 'Provider key of the LoginInfo.';
COMMENT ON COLUMN "users"."email" IS 'The email is the primary identification for a user account besides the LoginInfo.';
COMMENT ON COLUMN "users"."firstname" IS 'An optional firstname.';
COMMENT ON COLUMN "users"."lastname" IS 'An optional lastname.';
COMMENT ON COLUMN "users"."hasher" IS 'The hasher of the PasswordInfo.';
COMMENT ON COLUMN "users"."password" IS 'The password of the PasswordInfo.';
COMMENT ON COLUMN "users"."salt" IS 'The salt of the PasswordInfo.';
COMMENT ON COLUMN "users"."oauth1_token" IS 'The token of the OAuth1Info.';
COMMENT ON COLUMN "users"."oauth1_secret" IS 'The secret of the OAuth1Info.';
COMMENT ON COLUMN "users"."oauth2_access_token" IS 'The access token of the OAuth2Info.';
COMMENT ON COLUMN "users"."oauth2_token_type" IS 'The token type of the OAuth2Info.';
COMMENT ON COLUMN "users"."oauth2_expires" IS 'The expiration of the token for OAuth2Info.';
COMMENT ON COLUMN "users"."oauth2_refresh_token" IS 'The refresh token of the OAuth2Info.';
COMMENT ON COLUMN "users"."oauth2_params" IS 'The additional params of the OAuth2Info.';
COMMENT ON COLUMN "users"."avatar_url" IS 'Url of an optinal avatar image.';
COMMENT ON COLUMN "users"."activated" IS 'Whether the user has activated the account.';
COMMENT ON COLUMN "users"."active" IS 'Whether the account is active.';
COMMENT ON COLUMN "users"."created_at" IS 'A timestamp holding the information when the account was created.';
COMMENT ON COLUMN "users"."updated_at" IS 'A timestamp holding the information when the account was updated.';
COMMENT ON COLUMN "users"."admin" IS 'This flag indicates if the account is a system administrator.';
COMMENT ON COLUMN "users"."moderator" IS 'This flag indicates if the account is a system moderator.';

# --- !Downs

DROP TABLE "users";