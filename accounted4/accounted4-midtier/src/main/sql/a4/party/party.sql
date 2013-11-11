CREATE TABLE party(
  display_name       VARCHAR(64)  NOT NULL
 ,notes              TEXT
) INHERITS(base);

COMMENT ON TABLE party IS 'A party is a person or organization and is used as the interface to those entities.';

COMMENT ON COLUMN party.display_name IS 'The name to use for display purposes.';

SELECT ist_pk('party');


