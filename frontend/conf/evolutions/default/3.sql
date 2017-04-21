# --- !Ups

CREATE TABLE "friends"
(
  "user" uuid NOT NULL,
  "friend" uuid NOT NULL,
  "created_at" timestamp with time zone NOT NULL,
  CONSTRAINT "friends_unique_user_friend" PRIMARY KEY ("user", "friend"),
  CONSTRAINT "friends_fk_friend" FOREIGN KEY ("friend")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "friends_fk_user" FOREIGN KEY ("user")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE "friends" IS 'This tables stores the friends of each user.';
COMMENT ON COLUMN "friends"."user" IS 'The ID of the user';
COMMENT ON COLUMN "friends"."friend" IS 'The friend of the user.';
COMMENT ON COLUMN "friends"."created_at" IS 'When the friendship started.';

CREATE INDEX "friends_idx_user" ON "friends" ("user" ASC NULLS LAST);
COMMENT ON INDEX "friends_idx_user" IS 'Index on the user column.';

CREATE TABLE "friend_requests"
(
  "user" uuid NOT NULL,
  "friend" uuid NOT NULL,
  "created_at" timestamp with time zone NOT NULL,
  CONSTRAINT "friend_requests_unique_user_friend" PRIMARY KEY ("user", "friend"),
  CONSTRAINT "friends_request_fk_friends" FOREIGN KEY ("friend")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "friends_request_fk_user" FOREIGN KEY ("user")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE "friend_requests" IS 'This table contains the friend requests for a user.';
COMMENT ON COLUMN "friend_requests"."user" IS 'The user that received a friends request.';
COMMENT ON COLUMN "friend_requests"."friend" IS 'The user that submitted the friends request.';
COMMENT ON COLUMN "friend_requests"."created_at" IS 'When the friends request  was created.';

CREATE INDEX "friend_requests_idx_user" ON "friend_requests" ("user" ASC NULLS LAST);
COMMENT ON INDEX "friend_requests_idx_user" IS 'Index on the user column.';

CREATE TABLE "users_blocked"
(
  "user" uuid NOT NULL,
  "blocked" uuid NOT NULL,
  "created_at" timestamp with time zone NOT NULL,
  CONSTRAINT "users_blocked_unique_user_blocked" PRIMARY KEY ("user", "blocked"),
  CONSTRAINT "users_blocked_fk_blocked" FOREIGN KEY ("blocked")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "users_blocked_fk_user" FOREIGN KEY ("user")
      REFERENCES "users" ("user_id") MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);

COMMENT ON TABLE "users_blocked" IS 'This table contains users that have been blocked by other users.';
COMMENT ON COLUMN "users_blocked"."user" IS 'The user that has blocked another user.';
COMMENT ON COLUMN "users_blocked"."blocked" IS 'The user that has been blocked.';
COMMENT ON COLUMN "users_blocked"."created_at" IS 'When the block has been created.';

CREATE INDEX "users_blocked_idx_user" ON "users_blocked" ("user" ASC NULLS LAST);
COMMENT ON INDEX "users_blocked_idx_user" IS 'Index on the user column.';

# --- !Downs

DROP TABLE "friends";
DROP TABLE "friend_requests";
DROP TABLE "users_blocked";
DROP INDEX "friends_idx_user";
DROP INDEX "friend_requests_idx_user";
DROP INDEX "users_blocked_idx_user";