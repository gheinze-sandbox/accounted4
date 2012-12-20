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
