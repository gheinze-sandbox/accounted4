DROP TYPE IF EXISTS user_account_status CASCADE;
CREATE TYPE user_account_status AS ENUM('ACTIVE', 'LOCKED', 'RETIRED');

DROP TYPE IF EXISTS user_account_limited CASCADE;
CREATE TYPE user_account_limited AS (
   id            integer
  ,name          character varying(32)
  ,status        user_account_status
  ,display_name  character varying(64)
  ,email         character varying(64)
);



CREATE TABLE user_account(
  name               VARCHAR(32)  NOT NULL CONSTRAINT user_account_name UNIQUE
 ,encrypted_password TEXT         NOT NULL
 ,status             user_account_status  NOT NULL DEFAULT 'ACTIVE'::user_account_status
 ,display_name       VARCHAR(64)  NOT NULL
 ,email              VARCHAR(64)  NOT NULL
) INHERITS(base);

COMMENT ON TABLE user_account IS 'All operations performed in the application are associated with a "user_account" (a data owner). The user must authenticate in order to use system services and has access to data associated with the account.';

COMMENT ON COLUMN user_account.name IS 'The name of the user account used for login purposes';
COMMENT ON COLUMN user_account.encrypted_password IS 'The encrypted password used to log into this user account.';
COMMENT ON COLUMN user_account.status IS 'The status of an account will affect the operations it may perform. An account my be "ACTIVE", "LOCKED", "RETIRED", etc';
COMMENT ON COLUMN user_account.display_name IS 'The name to use for display or greeting purposes.';
COMMENT ON COLUMN user_account.email IS 'An address to which notifications may be sent.';


-- Add keys:

SELECT ist_pk('user_account');

SELECT ist_bk('user_account', ARRAY['name']);


-- Auditing:

SELECT audit.audit_table('user_account');





-- Interface:

CREATE OR REPLACE FUNCTION user_account_create(
        p_name          text
       ,p_password      text
       ,p_display_name  text
       ,p_email         text
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS integer AS $$

DECLARE

    v_id integer;

BEGIN

    INSERT INTO user_account(

         name
        ,encrypted_password
        ,display_name
        ,email
        ,owner_user_account_id
        ,audit_user_account_id

      ) VALUES (

         p_name
        ,pgcrypto.crypt( concat(p_name, p_password), pgcrypto.gen_salt('bf'))
        ,p_display_name
        ,p_email
        ,p_owner_user_account_id
        ,p_audit_user_account_id

      ) RETURNING id INTO v_id;

    RETURN v_id;

END;
$$ LANGUAGE plpgsql;





CREATE OR REPLACE FUNCTION user_account_modify(
        p_name          text
       ,p_password      text
       ,p_status        user_account_status
       ,p_display_name  text
       ,p_email         text
       ,p_user_account_id integer
       ,p_version         integer
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS integer AS $$

DECLARE

    v_version integer;

BEGIN

    PERFORM lock_record(
        'user_account'
       ,p_user_account_id
       ,p_version
       ,p_owner_user_account_id
       ,p_audit_user_account_id
      );

    UPDATE user_account
      SET name = p_name
         ,encrypted_password = pgcrypto.crypt(p_password, pgcrypto.gen_salt('bf'))
         ,status = p_status
         ,display_name = p_display_name
         ,email = p_email
         ,owner_user_account_id = p_owner_user_account_id
         ,audit_user_account_id = p_audit_user_account_id

      WHERE id = p_user_account_id
      RETURNING version INTO v_version;

    RETURN v_version;

END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION user_account_authenticate(
        p_name          text
       ,p_password      text
) RETURNS user_account AS $$

    SELECT *
      FROM user_account
      WHERE name = $1
        AND encrypted_password = pgcrypto.crypt( concat($1, $2), encrypted_password );

$$ LANGUAGE SQL;


    
-- A default system account

INSERT INTO user_account(name, encrypted_password, display_name, email)
    VALUES ('admin', pgcrypto.crypt( concat('admin', 'admin'), pgcrypto.gen_salt('bf')), 'Administrator', 'a4.admin@gheinze.com');

UPDATE user_account
  SET owner_user_account_id = id
     ,audit_user_account_id = id
  WHERE name = 'admin';



-- Admin account creation

WITH admin AS (SELECT id FROM user_account WHERE name = 'admin')
SELECT user_account_create(
       p_name         := 'glenn'
      ,p_password     := 'glenn'
      ,p_display_name := 'Glenn'
      ,p_email        := 'a4.admin@gheinze.com'
      ,p_owner_user_account_id := admin.id
      ,p_audit_user_account_id := admin.id
      )
  FROM admin;
