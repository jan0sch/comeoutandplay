# --- !Ups

CREATE TABLE "seabattles" (
  "game_id"  UUID  NOT NULL,
  "state"    JSONB NOT NULL,
  "owner"    UUID  NOT NULL,
  "opponent" UUID,
  CONSTRAINT "seabattles_pk" PRIMARY KEY ("game_id")
);

COMMENT ON TABLE "seabattles" IS 'This table contains the state for all seabattle games.';
COMMENT ON COLUMN "seabattles"."game_id" IS 'The unique identifier of a game.';
COMMENT ON COLUMN "seabattles"."owner" IS 'The id of the user that created the game.';
COMMENT ON COLUMN "seabattles"."opponent" IS 'The id of the user that is the owners opponent.';
COMMENT ON COLUMN "seabattles"."state" IS 'The current state of the game serialised as JSON.';

# --- !Downs

DROP TABLE "seabattles";
