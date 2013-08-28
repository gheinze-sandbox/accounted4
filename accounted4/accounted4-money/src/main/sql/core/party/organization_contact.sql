CREATE TABLE organization_contact(
  organization_id       INTEGER
  person_id             INTEGER
);

COMMENT ON TABLE organization IS 'Associate persons with an organization.';

SELECT ist_bk('organization_contact', ARRAY['organization_id', 'person_id']);