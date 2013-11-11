CREATE TABLE user_role(
  name         VARCHAR(64)  NOT NULL
 ,description  VARCHAR(64)
) INHERITS(base);

COMMENT ON TABLE user_role IS 'A user role represents a grouping of access permissions to application services which can be applied to a user..';

COMMENT ON COLUMN user_role.name IS 'The name representing the collection of grants.';
COMMENT ON COLUMN user_role.description IS 'A description of the purpose for this role.';


-- Add keys:

SELECT ist_pk('user_role');

SELECT ist_bk('user_role', ARRAY['name']);


-- Auditing:

SELECT audit.audit_table('user_role');




CREATE TABLE user_role_map(
  user_account_id  INTEGER  NOT NULL CONSTRAINT uarm_user_account_id_fk REFERENCES user_account(id)
 ,user_role_id     INTEGER  NOT NULL CONSTRAINT uarm_user_role_id_fk    REFERENCES user_role(id)
);

COMMENT ON TABLE user_role_map IS 'A mapping of roles granted to users. The union of all grants of all roles applied to a given user will determine application access.';

COMMENT ON COLUMN user_role_map.user_account_id IS 'The user for whom the grants of the role are to be applied.';
COMMENT ON COLUMN user_role_map.user_role_id IS 'A collection of grants to be applied to the user.';


-- Add keys:

SELECT ist_bk('user_role_map', ARRAY['user_account_id', 'user_role_id']);


-- Auditing:

SELECT audit.audit_table('user_role_map');




CREATE OR REPLACE FUNCTION user_role_create(
        p_name          text
       ,p_description   text
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS integer AS $$

DECLARE

    v_id integer;

BEGIN

    INSERT INTO user_role(

         name
        ,description
        ,owner_user_account_id
        ,audit_user_account_id

      ) VALUES (

         p_name
        ,p_description
        ,p_owner_user_account_id
        ,p_audit_user_account_id

      ) RETURNING id INTO v_id;

    RETURN v_id;

END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION user_role_delete(
        p_user_role_id  integer
) RETURNS integer AS $$

DECLARE

BEGIN

    DELETE FROM user_role
      WHERE id = p_user_role_id
    ;

END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION user_role_modify(
        p_name          text
       ,p_description   text
       ,p_user_role_id  integer
       ,p_version         integer
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS integer AS $$

DECLARE

    v_version integer;
    v_record_ownwer_id integer;

BEGIN

    v_version := lock_record(
        'user_role'
       ,p_user_role_id
       ,p_version
       ,p_owner_user_account_id
       ,p_audit_user_account_id
      );

    UPDATE user_role
      SET name = p_name
         ,description = p_description
         ,owner_user_account_id = p_owner_user_account_id
         ,audit_user_account_id = p_audit_user_account_id

      WHERE id = p_user_role_id
      RETURNING version INTO v_version;

    RETURN v_version;

END;
$$ LANGUAGE plpgsql;





CREATE OR REPLACE FUNCTION user_role_map(
        p_user_account_id       integer
       ,p_user_role_id          integer
) RETURNS void AS $$

DECLARE

BEGIN

    INSERT INTO user_role_map(user_account_id, user_role_id)
      VALUES(p_user_account_id, p_user_role_id)
    ;

END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION user_role_unmap(
        p_user_account_id       integer
       ,p_user_role_id          integer
) RETURNS void AS $$

DECLARE

BEGIN

    DELETE FROM user_role_map
      WHERE user_account_id = p_user_account_id
        AND user_role_id = p_user_role_id
    ;

END;
$$ LANGUAGE plpgsql;





CREATE OR REPLACE FUNCTION user_role_list(
        p_user_account_id       integer
) RETURNS SETOF user_role AS $$

    SELECT r.*
      FROM user_role_map m
          ,user_role r
      WHERE m.user_account_id = $1
        AND r.id = m.user_role_id
    ;

$$ LANGUAGE SQL;
