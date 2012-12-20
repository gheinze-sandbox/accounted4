CREATE TABLE user_delegate_map(
  user_account_id           INTEGER  NOT NULL CONSTRAINT user_delegate_ua_id_fk REFERENCES user_account(id)
 ,delegate_user_account_id  INTEGER  NOT NULL CONSTRAINT user_delegate_dua_id_fk REFERENCES user_account(id)
);

COMMENT ON TABLE user_delegate_map IS 'Specify which users may operate an account on behalf of another user.';

COMMENT ON COLUMN user_delegate_map.user_account_id IS 'The user granting permission for another to operate this account.';
COMMENT ON COLUMN user_delegate_map.delegate_user_account_id IS 'The user account which may operate as the user_account_id.';


-- Add keys:

SELECT ist_bk('user_delegate_map', ARRAY['user_account_id', 'delegate_user_account_id']);


-- Auditing:

SELECT audit.audit_table('user_delegate_map');




CREATE OR REPLACE FUNCTION user_delegate_map(
        p_user_account_id           integer
       ,p_delegate_user_account_id  integer
) RETURNS void AS $$

DECLARE

BEGIN

    INSERT INTO user_delegate_map(user_account_id, delegate_user_account_id)
      VALUES(p_user_account_id, p_delegate_user_account_id)
    ;

END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION user_delegate_unmap(
        p_user_account_id           integer
       ,p_delegate_user_account_id  integer
) RETURNS void AS $$

DECLARE

BEGIN

    DELETE FROM user_delegate_map
      WHERE user_account_id = p_user_account_id
        AND delegate_user_account_id = p_delegate_user_account_id
    ;

END;
$$ LANGUAGE plpgsql;





CREATE OR REPLACE FUNCTION user_delegate_list(
        p_user_account_id       integer
) RETURNS SETOF user_account_limited AS $$

    SELECT u.id
          ,u.name
          ,u.status
          ,u.display_name
          ,u.email

      FROM user_role_map m
          ,user_account u

      WHERE m.user_account_id = $1
        AND u.id = m.user_role_id
    ;

$$ LANGUAGE SQL;
