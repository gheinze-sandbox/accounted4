CREATE TABLE person_phone_map(
  person_id      INTEGER
 ,phone_id       INTEGER
);

COMMENT ON TABLE person_phone_map IS 'Associate a phone with a person.';

SELECT ist_bk('person_phone_map', ARRAY['person_id', 'phone_id']);
