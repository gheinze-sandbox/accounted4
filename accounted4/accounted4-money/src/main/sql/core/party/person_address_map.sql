CREATE TABLE person_address_map(
  person_id        INTEGER
 ,address_id       INTEGER
);

COMMENT ON TABLE person_address_map IS 'Associate an address with a person.';

SELECT ist_bk('person_address_map', ARRAY['person_id', 'address_id']);
