CREATE TABLE base(
  id                    INTEGER
 ,version               INTEGER    DEFAULT 1
 ,owner_user_account_id INTEGER
 ,audit_user_account_id INTEGER
 ,last_modified_time    TIMESTAMP  DEFAULT now()
);

COMMENT ON TABLE base IS 'Meta information of use for all tables in order to support auditing functions and concurrency support.  This table should never be inserted to directly. It is to serve as a parent table for inheritance.';

COMMENT ON COLUMN base.id IS 'A unique identifier for records within a table.';
COMMENT ON COLUMN base.version IS 'The version of the record (typically equal to the number of times it was modified), used to address concurrency issues';
COMMENT ON COLUMN base.owner_user_account_id IS 'The owner of this record. Used to determine visibility for logically partitioned data sets.';
COMMENT ON COLUMN base.audit_user_account_id IS 'The creator of this record. Typically the same as the owner, unless someone is operating on the owner''s behalf. Typically used for auditing purposes.';
COMMENT ON COLUMN base.last_modified_time IS 'The timestamp of when the data record was last modified, used for auditing purposes.';




-- This table is only to be used for inheritence: don't allow direct input
CREATE RULE base_insert AS ON INSERT TO base DO INSTEAD NOTHING;




CREATE OR REPLACE FUNCTION trigger_base_update() RETURNS trigger AS $$
BEGIN
    new.last_modified_time := now();
    new.version := old.version + 1;
    RETURN new;
END;
$$ LANGUAGE plpgsql;






CREATE OR REPLACE FUNCTION verify_delegation(
        p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS void AS $$

DECLARE

BEGIN

    -- Verify the person operating the account may operate for the data owner
    IF ( p_owner_user_account_id != p_audit_user_account_id ) THEN

        IF NOT EXISTS(
            SELECT 1
              FROM user_delegate_map
              WHERE user_account_id = p_owner_user_account_id
                AND delegate_user_account_id = p_audit_user_account_id
          )
        THEN
          RAISE EXCEPTION 'Current user may not act as a delegate for this data';
        END IF;

    END IF;

    RETURN;

END;
$$ LANGUAGE plpgsql;




CREATE OR REPLACE FUNCTION lock_record(
        p_table_name            text
       ,p_id                    integer
       ,p_version               integer
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) RETURNS void AS $$

DECLARE

    v_version integer;
    v_record_ownwer_id integer;

BEGIN
    
    PERFORM verify_delegation(p_owner_user_account_id, p_audit_user_account_id);

    -- Lock the record
    EXECUTE 'SELECT version, owner_user_account_id' ||
            '  FROM '       || p_table_name ||
            '  WHERE id = ' || p_id ||
            '  FOR UPDATE' 
        INTO v_version, v_record_ownwer_id;


    IF ( v_record_ownwer_id != p_owner_user_account_id ) THEN
        RAISE EXCEPTION 'Cannot modify data owned by another user';
    END IF;


    IF ( v_version != p_version ) THEN
        RAISE EXCEPTION 'Concurrent update by another session, please refresh record to get latest values and try again';
    END IF;


    RETURN;

END;
$$ LANGUAGE plpgsql;


COMMENT ON FUNCTION lock_record(
        p_table_name            text
       ,p_id                    integer
       ,p_version               integer
       ,p_owner_user_account_id integer
       ,p_audit_user_account_id integer
) IS $comment$

  DESCR:
 
  Lock a record for update to ensure no concurrency conflicts.
  Assumes the table inherits from base.
  Verifies that the owner user account has permission to modify this record.
  Verifies that the operating user is permitted by owning user to operate on their behalf.
   
$comment$;