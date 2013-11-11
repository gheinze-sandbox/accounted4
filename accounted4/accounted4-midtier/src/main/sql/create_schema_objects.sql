-- Dynamic script generation start 
select now();                     
--------------------------- 
-- Processing File: .\core\extensions.sql     
--------------------------- 
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
--------------------------- 
-- Processing File: .\core\purge_schemas.sql     
--------------------------- 
-- =============================================
-- == Schema creation:
-- ==   o tia:   core application container
-- ==   o audit: auditing records
-- =============================================
DROP SCHEMA IF EXISTS tia CASCADE;
DROP SCHEMA IF EXISTS a4 CASCADE;
DROP SCHEMA IF EXISTS audit CASCADE;
DROP SCHEMA IF EXISTS spring_security CASCADE;
--------------------------- 
-- Processing File: .\core\create_schemas.sql     
--------------------------- 
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
--------------------------- 
-- Processing File: .\core\audit\switch_user_to_audit.sql;     
--------------------------- 
SET search_path=audit,public;

SET SESSION AUTHORIZATION 'a4';
--------------------------- 
-- Processing File: .\core\audit\audit.sql     
--------------------------- 
-- An audit history is important on most tables. Provide an audit trigger that logs to
-- a dedicated audit table for the major relations.
--
-- This file should be generic and not depend on application roles or structures,
-- as it's being listed here:
--
--    https://wiki.postgresql.org/wiki/Audit_trigger_91plus    
--
-- This trigger was originally based on
--   http://wiki.postgresql.org/wiki/Audit_trigger
-- but has been completely rewritten.

--CREATE EXTENSION IF NOT EXISTS hstore;

--CREATE SCHEMA audit;
--REVOKE ALL ON SCHEMA audit FROM public;

-- COMMENT ON SCHEMA audit IS 'Out-of-table audit/history logging tables and trigger functions';

--
-- Audited data. Lots of information is available, it's just a matter of how much
-- you really want to record. See:
--
--   http://www.postgresql.org/docs/9.1/static/functions-info.html
--
-- Remember, every column you add takes up more audit table space and slows audit
-- inserts.
--
-- Every index you add has a big impact too, so avoid adding indexes to the
-- audit table unless you REALLY need them. The hstore GIST indexes are
-- particularly expensive.
--
-- It is sometimes worth copying the audit table, or a coarse subset of it that
-- you're interested in, into a temporary table where you CREATE any useful
-- indexes and do your analysis.
--
CREATE TABLE logged_actions (
    event_id          bigserial primary key,
    schema_name       text not null,
    table_name        text not null,
    relid             oid not null,
    session_user_name text,
    action_tstamp_tx  TIMESTAMP WITH TIME ZONE NOT NULL,
    action_tstamp_stm TIMESTAMP WITH TIME ZONE NOT NULL,
    action_tstamp_clk TIMESTAMP WITH TIME ZONE NOT NULL,
    transaction_id    bigint,
    application_name  text,
    client_addr       inet,
    client_port       integer,
    client_query      text not null,
    action            TEXT NOT NULL CHECK (action IN ('I','D','U', 'T')),
    row_data          hstore,
    changed_fields    hstore,
    statement_only    boolean not null
);

--REVOKE ALL ON audit.logged_actions FROM public;

COMMENT ON TABLE logged_actions IS 'History of auditable actions on audited tables, from audit.if_modified_func()';
COMMENT ON COLUMN logged_actions.event_id IS 'Unique identifier for each auditable event';
COMMENT ON COLUMN logged_actions.schema_name IS 'Database schema audited table for this event is in';
COMMENT ON COLUMN logged_actions.table_name IS 'Non-schema-qualified table name of table event occured in';
COMMENT ON COLUMN logged_actions.relid IS 'Table OID. Changes with drop/create. Get with ''tablename''::regclass';
COMMENT ON COLUMN logged_actions.session_user_name IS 'Login / session user whose statement caused the audited event';
COMMENT ON COLUMN logged_actions.action_tstamp_tx IS 'Transaction start timestamp for tx in which audited event occurred';
COMMENT ON COLUMN logged_actions.action_tstamp_stm IS 'Statement start timestamp for tx in which audited event occurred';
COMMENT ON COLUMN logged_actions.action_tstamp_clk IS 'Wall clock time at which audited event''s trigger call occurred';
COMMENT ON COLUMN logged_actions.transaction_id IS 'Identifier of transaction that made the change. May wrap, but unique paired with action_tstamp_tx.';
COMMENT ON COLUMN logged_actions.client_addr IS 'IP address of client that issued query. Null for unix domain socket.';
COMMENT ON COLUMN logged_actions.client_port IS 'Remote peer IP port address of client that issued query. Undefined for unix socket.';
COMMENT ON COLUMN logged_actions.client_query IS 'Top-level query that caused this auditable event. May be more than one statement.';
COMMENT ON COLUMN logged_actions.application_name IS 'Application name set when this audit event occurred. Can be changed in-session by client.';
COMMENT ON COLUMN logged_actions.action IS 'Action type; I = insert, D = delete, U = update, T = truncate';
COMMENT ON COLUMN logged_actions.row_data IS 'Record value. Null for statement-level trigger. For INSERT this is the new tuple. For DELETE and UPDATE it is the old tuple.';
COMMENT ON COLUMN logged_actions.changed_fields IS 'New values of fields changed by UPDATE. Null except for row-level UPDATE events.';
COMMENT ON COLUMN logged_actions.statement_only IS '''t'' if audit event is from an FOR EACH STATEMENT trigger, ''f'' for FOR EACH ROW';

CREATE INDEX logged_actions_relid_idx ON logged_actions(relid);
CREATE INDEX logged_actions_action_tstamp_tx_stm_idx ON logged_actions(action_tstamp_stm);
CREATE INDEX logged_actions_action_idx ON logged_actions(action);

CREATE OR REPLACE FUNCTION if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    audit_row logged_actions;
    include_values boolean;
    log_diffs boolean;
    h_old hstore;
    h_new hstore;
    excluded_cols text[] = ARRAY[]::text[];
BEGIN
    IF TG_WHEN <> 'AFTER' THEN
        RAISE EXCEPTION 'audit.if_modified_func() may only run as an AFTER trigger';
    END IF;

    audit_row = ROW(
        nextval('logged_actions_event_id_seq'),       -- event_id
        TG_TABLE_SCHEMA::text,                        -- schema_name
        TG_TABLE_NAME::text,                          -- table_name
        TG_RELID,                                     -- relation OID for much quicker searches
        session_user::text,                           -- session_user_name
        current_timestamp,                            -- action_tstamp_tx
        statement_timestamp(),                        -- action_tstamp_stm
        clock_timestamp(),                            -- action_tstamp_clk
        txid_current(),                               -- transaction ID
        (SELECT setting FROM pg_settings WHERE name = 'application_name'),
        inet_client_addr(),                           -- client_addr
        inet_client_port(),                           -- client_port
        current_query(),                              -- top-level query or queries (if multistatement) from client
        substring(TG_OP,1,1),                         -- action
        NULL, NULL,                                   -- row_data, changed_fields
        'f'                                           -- statement_only
        );

    IF NOT TG_ARGV[0]::boolean IS DISTINCT FROM 'f'::boolean THEN
        audit_row.client_query = NULL;
    END IF;

    IF TG_ARGV[1] IS NOT NULL THEN
        excluded_cols = TG_ARGV[1]::text[];
    END IF;
    
    IF (TG_OP = 'UPDATE' AND TG_LEVEL = 'ROW') THEN
        audit_row.row_data = hstore(OLD.*);
        audit_row.changed_fields =  (hstore(NEW.*) - audit_row.row_data) - excluded_cols;
        IF audit_row.changed_fields = hstore('') THEN
            -- All changed fields are ignored. Skip this update.
            RETURN NULL;
        END IF;
    ELSIF (TG_OP = 'DELETE' AND TG_LEVEL = 'ROW') THEN
        audit_row.row_data = hstore(OLD.*) - excluded_cols;
    ELSIF (TG_OP = 'INSERT' AND TG_LEVEL = 'ROW') THEN
        audit_row.row_data = hstore(NEW.*) - excluded_cols;
    ELSIF (TG_LEVEL = 'STATEMENT' AND TG_OP IN ('INSERT','UPDATE','DELETE','TRUNCATE')) THEN
        audit_row.statement_only = 't';
    ELSE
        RAISE EXCEPTION '[if_modified_func] - Trigger func added as trigger for unhandled case: %, %',TG_OP, TG_LEVEL;
        RETURN NULL;
    END IF;
    INSERT INTO logged_actions VALUES (audit_row.*);
    RETURN NULL;
END;
$body$
LANGUAGE plpgsql;
--SECURITY DEFINER
--SET search_path = pg_catalog, public;


COMMENT ON FUNCTION if_modified_func() IS $body$
Track changes to a table at the statement and/or row level.

Optional parameters to trigger in CREATE TRIGGER call:

param 0: boolean, whether to log the query text. Default 't'.

param 1: text[], columns to ignore in updates. Default [].

         Updates to ignored cols are omitted from changed_fields.

         Updates with only ignored cols changed are not inserted
         into the audit log.

         Almost all the processing work is still done for updates
         that ignored. If you need to save the load, you need to use
         WHEN clause on the trigger instead.

         No warning or error is issued if ignored_cols contains columns
         that do not exist in the target table. This lets you specify
         a standard set of ignored columns.

There is no parameter to disable logging of values. Add this trigger as
a 'FOR EACH STATEMENT' rather than 'FOR EACH ROW' trigger if you do not
want to log row values.

Note that the user name logged is the login role for the session. The audit trigger
cannot obtain the active role because it is reset by the SECURITY DEFINER invocation
of the audit trigger its self.
$body$;



CREATE OR REPLACE FUNCTION audit_table(target_table regclass, audit_rows boolean, audit_query_text boolean, ignored_cols text[]) RETURNS void AS $body$
DECLARE
  stm_targets text = 'INSERT OR UPDATE OR DELETE OR TRUNCATE';
  _q_txt text;
  _ignored_cols_snip text = '';
BEGIN
    EXECUTE 'DROP TRIGGER IF EXISTS audit_trigger_row ON ' || quote_ident(target_table::text);
    EXECUTE 'DROP TRIGGER IF EXISTS audit_trigger_stm ON ' || quote_ident(target_table::text);

    IF audit_rows THEN
        IF array_length(ignored_cols,1) > 0 THEN
            _ignored_cols_snip = ', ' || quote_literal(ignored_cols);
        END IF;
        _q_txt = 'CREATE TRIGGER audit_trigger_row AFTER INSERT OR UPDATE OR DELETE ON ' || 
                 quote_ident(target_table::text) || 
                 ' FOR EACH ROW EXECUTE PROCEDURE if_modified_func(' ||
                 quote_literal(audit_query_text) || _ignored_cols_snip || ');';
        RAISE NOTICE '%',_q_txt;
        EXECUTE _q_txt;
        stm_targets = 'TRUNCATE';
    ELSE
    END IF;

    _q_txt = 'CREATE TRIGGER audit_trigger_stm AFTER ' || stm_targets || ' ON ' ||
             quote_ident(target_table::text) ||
             ' FOR EACH STATEMENT EXECUTE PROCEDURE if_modified_func('||
             quote_literal(audit_query_text) || ');';
    RAISE NOTICE '%',_q_txt;
    EXECUTE _q_txt;

END;
$body$
language 'plpgsql';

COMMENT ON FUNCTION audit_table(regclass, boolean, boolean, text[]) IS $body$
Add auditing support to a table.

Arguments:
   target_table:     Table name, schema qualified if not on search_path
   audit_rows:       Record each row change, or only audit at a statement level
   audit_query_text: Record the text of the client query that triggered the audit event?
   ignored_cols:     Columns to exclude from update diffs, ignore updates that change only ignored cols.
$body$;

-- Pg doesn't allow variadic calls with 0 params, so provide a wrapper
CREATE OR REPLACE FUNCTION audit_table(target_table regclass, audit_rows boolean, audit_query_text boolean) RETURNS void AS $body$
SELECT audit_table($1, $2, $3, ARRAY[]::text[]);
$body$ LANGUAGE SQL;

-- And provide a convenience call wrapper for the simplest case
-- of row-level logging with no excluded cols and query logging enabled.
--
CREATE OR REPLACE FUNCTION audit_table(target_table regclass) RETURNS void AS $$
SELECT audit_table($1, BOOLEAN 't', BOOLEAN 't');
$$ LANGUAGE 'sql';

COMMENT ON FUNCTION audit_table(regclass) IS $body$
Add auditing support to the given table. Row-level changes will be logged with full client query text. No cols are ignored.
$body$;
--------------------------- 
-- Processing File: .\core\spring_security\switch_user_to_spring_security.sql;     
--------------------------- 
SET search_path=spring_security;

SET SESSION AUTHORIZATION 'spring_security';
--------------------------- 
-- Processing File: .\core\spring_security\spring_security.sql     
--------------------------- 

-- These definitions are copied from:
-- http://docs.spring.io/spring-security/site/docs/3.2.0.CI-SNAPSHOT/reference/htmlsingle/#appendix-schema


-- User schema:

create table users(
    username varchar(64) not null primary key,
    password varchar(64) not null,
    enabled boolean not null
)
;

create table authorities (
    username varchar(64) not null,
    authority varchar(64) not null,
    constraint fk_authorities_users foreign key(username) references users(username)
)
;

create unique index ix_auth_username on authorities (username,authority)
;


-- Group authorities:

create table groups (
  id SERIAL primary key,
  group_name varchar(64) not null
)
;

create table group_authorities (
  group_id SERIAL primary key,
  authority varchar(64) not null,
  constraint fk_group_authorities_group foreign key(group_id) references groups(id)
)
;

create table group_members (
  id SERIAL primary key,
  username varchar(64) not null,
  group_id bigint not null,
  constraint fk_group_members_group foreign key(group_id) references groups(id)
)
;

-- Remember me

create table persistent_logins (
  username varchar(64) not null,
  series varchar(64) primary key,
  token varchar(64) not null,
  last_used timestamp not null
)
;

-- ACL

create table acl_sid(
  id bigserial not null primary key,
  principal boolean not null,
  sid varchar(100) not null,
  constraint unique_uk_1 unique(sid,principal)
)
;

create table acl_class(
  id bigserial not null primary key,
  class varchar(100) not null,
  constraint unique_uk_2 unique(class)
)
;

create table acl_object_identity(
  id bigserial primary key,
  object_id_class bigint not null,
  object_id_identity bigint not null,
  parent_object bigint,
  owner_sid bigint,
  entries_inheriting boolean not null,
  constraint unique_uk_3 unique(object_id_class,object_id_identity),
  constraint foreign_fk_1 foreign key(parent_object) references acl_object_identity(id),
  constraint foreign_fk_2 foreign key(object_id_class) references acl_class(id),
  constraint foreign_fk_3 foreign key(owner_sid) references acl_sid(id)
)
;

create table acl_entry(
  id bigserial primary key,
  acl_object_identity bigint not null,
  ace_order int not null,
  sid bigint not null,
  mask integer not null,
  granting boolean not null,
  audit_success boolean not null,
  audit_failure boolean not null,
  constraint unique_uk_4 unique(acl_object_identity,ace_order),
  constraint foreign_fk_4 foreign key(acl_object_identity)
      references acl_object_identity(id),
  constraint foreign_fk_5 foreign key(sid) references acl_sid(id)
)
;
--------------------------- 
-- Processing File: .\core\spring_security\seed_users.sql     
--------------------------- 
-- Create an admin account "a4admin" with password "a42day"

insert into spring_security.users values('a4admin', '$2a$10$cJw2PP46Q20dRDz5aXlGb.lDSM.c2XtJMtoft2Y5xz.CINBdqN3ZC', true)
;
insert into spring_security.authorities values('a4admin', 'ROLE_USER')
;
insert into spring_security.authorities values('a4admin', 'ROLE_ADMIN')
;
--------------------------- 
-- Processing File: .\a4\switch_user_to_a4.sql;     
--------------------------- 
SET search_path=a4,audit,public;

SET SESSION AUTHORIZATION 'a4';--------------------------- 
-- Processing File: .\a4\installation_support_tools.sql     
--------------------------- 
---------------
-- Installation Support Tools
---------------

CREATE OR REPLACE FUNCTION ist_pk(p_table_name text) RETURNS void AS $$
DECLARE

    v_sequence_name   text := p_table_name || '_seq';
    v_constraint_name text := p_table_name || '_pk';

BEGIN

    EXECUTE 'CREATE SEQUENCE ' || v_sequence_name;

    EXECUTE 'ALTER TABLE ' || p_table_name || ' ALTER COLUMN id SET DEFAULT nextval(''' || v_sequence_name || ''')';

    -- Index is created implicitly by PK constraint
    -- EXECUTE 'CREATE UNIQUE INDEX ' || v_index_name || ' ON ' || p_table_name || '(id)';

    EXECUTE 'ALTER TABLE ' || p_table_name || ' ADD CONSTRAINT ' || v_constraint_name || ' PRIMARY KEY(id)';

    EXECUTE 'CREATE TRIGGER ' || p_table_name || '_update BEFORE UPDATE ON ' || p_table_name || ' FOR EACH ROW EXECUTE PROCEDURE trigger_base_update();';

    RETURN;

END;
$$ LANGUAGE plpgsql;



COMMENT ON FUNCTION ist_pk(p_table_name text) IS $comment$

  IN:  a table name
  OUT: -

  DESCR:
    
  Perform the task similar to a SERIAL data type, plus add a trigger.

   1. Create a sequence:  <p_table_name>_seq
   2. Default the id column to the sequence.
   3. Creates a unique index on the "id" column:   <p_table_name>_pk
   4. Create a primary key constraint: pk_<p_table_name>
   5. Add a trigger to update values for columns inherited from base

   Assumes inheritance from "base".
   
$comment$;



/*
 * IN:  a table name
 *      an array of columns comprising the unique business key of the table
 * OUT: -
 *
 * DESCR: creates a unique index "table_name_bk" based on the list of input columns
 */
CREATE OR REPLACE FUNCTION ist_bk(p_table_name varchar, p_column_names varchar[]) RETURNS void AS $$
DECLARE

    v_column_list varchar := p_column_names[1];
    v_index_name varchar := p_table_name || '_bk';

BEGIN

    FOR i IN array_lower(p_column_names,1)+1 .. array_upper(p_column_names,1) LOOP
        v_column_list := v_column_list || ', ' || p_column_names[i];
        -- RAISE NOTICE '%', v_column_list;
    END LOOP;
    

    EXECUTE 'CREATE UNIQUE INDEX ' || v_index_name || ' ON ' || p_table_name
            || '(' || v_column_list || ')';

    RETURN;

END;
$$ LANGUAGE plpgsql;


COMMENT ON FUNCTION ist_bk(p_table_name varchar, p_column_names varchar[]) IS $comment$

  IN:  a table name
       an array of columns comprising the unique business key of the table
  OUT: -

  DESCR:
 
  Creates a unique index "table_name_bk" based on the list of input columns.
   
$comment$;
--------------------------- 
-- Processing File: .\a4\base.sql     
--------------------------- 
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
   
$comment$;--------------------------- 
-- Processing File: .\a4\user_account\user_account.sql     
--------------------------- 
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
--------------------------- 
-- Processing File: .\a4\user_account\user_role.sql     
--------------------------- 
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
--------------------------- 
-- Processing File: .\a4\user_account\user_delegate.sql     
--------------------------- 
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
--------------------------- 
-- Processing File: .\a4\party\party.sql     
--------------------------- 
CREATE TABLE party(
  display_name       VARCHAR(64)  NOT NULL
 ,notes              TEXT
) INHERITS(base);

COMMENT ON TABLE party IS 'A party is a person or organization and is used as the interface to those entities.';

COMMENT ON COLUMN party.display_name IS 'The name to use for display purposes.';

SELECT ist_pk('party');


--------------------------- 
-- Processing File: .\a4\party\person.sql     
--------------------------- 
CREATE TABLE person(
  last_name       VARCHAR(64)  NOT NULL
 ,first_name      VARCHAR(64)  NOT NULL
 ,dob             DATE
 ,sin             CHAR(11)
) INHERITS(party);

COMMENT ON TABLE person IS 'A person can act in many roles within the application: mortgager, mortgagee, contact, vendor, ...';

COMMENT ON COLUMN person.last_name IS 'Cuurent last name.';
COMMENT ON COLUMN person.last_name IS 'Cuurent first name.';


-- Add keys:

-- Index is created implicitly by PK constraint
ALTER TABLE person ADD CONSTRAINT person_pk PRIMARY KEY(id);

-- PERSON shares the id generation sequence with PARTY
ALTER TABLE person ALTER COLUMN id SET DEFAULT nextval('party_seq');


-- TODO: capture UPDATE for display name...

-- When insertint a PERSON, capture the key in PARTY as well
CREATE OR REPLACE FUNCTION trigger_person_insert() RETURNS trigger AS $$
BEGIN
    INSERT INTO party(id, display_name)
      VALUES (NEW.id, NEW.last_name || ', ' || NEW.first_name);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Link the trigger to the proc
CREATE TRIGGER person_insert_party_info
  AFTER INSERT ON person
  FOR EACH ROW
  EXECUTE PROCEDURE trigger_person_insert();


SELECT ist_bk('person', ARRAY['last_name', 'first_name']);

CREATE UNIQUE INDEX person_sin_uk ON person(sin);


-- Auditing:

SELECT audit.audit_table('person');
--------------------------- 
-- Processing File: .\a4\party\organization.sql     
--------------------------- 
CREATE TABLE organization(
  name       VARCHAR(64)  NOT NULL
) INHERITS(party);

COMMENT ON TABLE organization IS 'An organization.';

COMMENT ON COLUMN organization.name IS 'The name of the organization which may be acting in many roles within the application: mortgager, mortgagee, contact, vendor, husband-wife grouping, etc';


-- Add keys:

-- Index is created implicitly by PK constraint
ALTER TABLE organization ADD CONSTRAINT organization_pk PRIMARY KEY(id);

-- organization shares the id generation sequence with PARTY
ALTER TABLE organization ALTER COLUMN id SET DEFAULT nextval('party_seq');


-- TODO: capture UPDATE for display name...

-- When insertint a PERSON, capture the key in PARTY as well
CREATE OR REPLACE FUNCTION trigger_organization_insert() RETURNS trigger AS $$
BEGIN
    INSERT INTO party(id, display_name)
      VALUES (NEW.id, NEW.name);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Link the trigger to the proc
CREATE TRIGGER organization_insert_party_info
  AFTER INSERT ON organization
  FOR EACH ROW
  EXECUTE PROCEDURE trigger_organization_insert();



SELECT ist_bk('organization', ARRAY['name']);


-- Auditing:

SELECT audit.audit_table('organization');--------------------------- 
-- Processing File: .\a4\party\organization_contact.sql     
--------------------------- 
CREATE TABLE organization_contact(
  organization_id       INTEGER
 ,person_id             INTEGER
);

COMMENT ON TABLE organization IS 'Associate persons with an organization.';

SELECT ist_bk('organization_contact', ARRAY['organization_id', 'person_id']);
--------------------------- 
-- Processing File: .\a4\party\address.sql     
--------------------------- 
CREATE TABLE address(
  address1        VARCHAR(64)
 ,address2        VARCHAR(64)  
 ,city            VARCHAR(64)
 ,postal_code     CHAR(7)
 ,province        CHAR(2)
 ,notes           TEXT
) INHERITS(base);

COMMENT ON TABLE address IS 'A location for entities such as party, property, and asset';

SELECT ist_pk('address');
SELECT ist_bk('address', ARRAY['address1', 'city', 'province']);
--------------------------- 
-- Processing File: .\a4\party\person_address_map.sql     
--------------------------- 
CREATE TABLE person_address_map(
  person_id        INTEGER
 ,address_id       INTEGER
);

COMMENT ON TABLE person_address_map IS 'Associate an address with a person.';

SELECT ist_bk('person_address_map', ARRAY['person_id', 'address_id']);
--------------------------- 
-- Processing File: .\a4\party\phone.sql     
--------------------------- 
CREATE TABLE phone(
  phone_type        VARCHAR(16)
 ,phone_number      VARCHAR(64)  
 ,notes             TEXT
) INHERITS(base);

COMMENT ON TABLE phone IS 'Contact information.';

SELECT ist_pk('phone');
SELECT ist_bk('phone', ARRAY['phone_type', 'phone_number']);
--------------------------- 
-- Processing File: .\a4\party\person_phone_map.sql     
--------------------------- 
CREATE TABLE person_phone_map(
  person_id      INTEGER
 ,phone_id       INTEGER
);

COMMENT ON TABLE person_phone_map IS 'Associate a phone with a person.';

SELECT ist_bk('person_phone_map', ARRAY['person_id', 'phone_id']);
--------------------------- 
-- Processing File: .\a4\party\email.sql     
--------------------------- 
CREATE TABLE email(
  email_type        VARCHAR(16)
 ,email_address     VARCHAR(64)  
 ,notes             TEXT
) INHERITS(base);

COMMENT ON TABLE email IS 'Contact information.';

SELECT ist_pk('email');
SELECT ist_bk('email', ARRAY['email_type', 'email_address']);
--------------------------- 
-- Processing File: .\a4\party\person_email_map.sql     
--------------------------- 
CREATE TABLE person_email_map(
  person_id      INTEGER
 ,email_id       INTEGER
);

COMMENT ON TABLE person_email_map IS 'Associate an email with a person.';

SELECT ist_bk('person_email_map', ARRAY['person_id', 'email_id']);
