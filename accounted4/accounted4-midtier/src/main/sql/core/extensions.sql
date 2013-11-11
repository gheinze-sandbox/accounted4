-- =============================================
-- DB extensions
-- =============================================
CREATE EXTENSION IF NOT EXISTS plpgsql;
CREATE EXTENSION IF NOT EXISTS hstore;

DO $$
BEGIN

    IF NOT EXISTS(
        SELECT schema_name
          FROM information_schema.schemata
          WHERE schema_name = 'pgcrypto'
      )
    THEN
      EXECUTE 'CREATE SCHEMA pgcrypto';
    END IF;

END
$$; 
   
CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA pgcrypto;

GRANT USAGE ON SCHEMA pgcrypto TO PUBLIC;
