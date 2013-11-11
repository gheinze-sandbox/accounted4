DO $$
BEGIN

    IF NOT EXISTS(
        SELECT usename
          FROM pg_shadow
          WHERE usename = 'a4'
      )
    THEN
      EXECUTE 'CREATE ROLE a4 LOGIN PASSWORD ''a4''';
    END IF;

    IF NOT EXISTS(
        SELECT usename
          FROM pg_shadow
          WHERE usename = 'spring_security'
      )
    THEN
      EXECUTE 'CREATE ROLE spring_security LOGIN PASSWORD ''spring_security''';
    END IF;

END
$$; 

CREATE SCHEMA a4 AUTHORIZATION a4;

CREATE SCHEMA spring_security AUTHORIZATION spring_security;

CREATE SCHEMA audit AUTHORIZATION a4;
COMMENT ON SCHEMA audit IS 'Out-of-table audit/history logging tables and trigger functions';
