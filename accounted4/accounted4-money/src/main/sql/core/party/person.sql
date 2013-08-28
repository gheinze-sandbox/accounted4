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
ALTER TABLE person ALTER COLUMN id SET DEFAULT nextval(party_seq);


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
