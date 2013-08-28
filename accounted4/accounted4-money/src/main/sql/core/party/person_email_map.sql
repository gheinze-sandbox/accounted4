CREATE TABLE person_email_map(
  person_id      INTEGER
 ,email_id       INTEGER
);

COMMENT ON TABLE person_email_map IS 'Associate an email with a person.';

SELECT ist_bk('person_email_map', ARRAY['person_id', 'email_id']);