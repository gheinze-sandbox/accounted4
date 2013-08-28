CREATE TABLE organization(
  name       VARCHAR(64)  NOT NULL
) INHERITS(party);

COMMENT ON TABLE organization IS 'An organization.';

COMMENT ON COLUMN organization.name IS 'The name of the organization which may be acting in many roles within the application: mortgager, mortgagee, contact, vendor, husband-wife grouping, etc';


-- Add keys:

-- Index is created implicitly by PK constraint
ALTER TABLE organization ADD CONSTRAINT organization_pk PRIMARY KEY(id);

-- organization shares the id generation sequence with PARTY
ALTER TABLE organization ALTER COLUMN id SET DEFAULT nextval(party_seq);


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

SELECT audit.audit_table('organization');