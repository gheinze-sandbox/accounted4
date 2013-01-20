DO $$
BEGIN

    IF NOT EXISTS(
        SELECT usename
          FROM pg_shadow
          WHERE usename = 'tia'
      )
    THEN
      EXECUTE 'CREATE ROLE tia LOGIN PASSWORD ''tia''';
    END IF;

END
$$; 

CREATE SCHEMA tia AUTHORIZATION tia;

CREATE SCHEMA audit AUTHORIZATION tia;