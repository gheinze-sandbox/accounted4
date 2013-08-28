CREATE TABLE phone(
  phone_type        VARCHAR(16)
 ,phone_number      VARCHAR(64)  
 ,notes             TEXT
) INHERITS(base);

COMMENT ON TABLE phone IS 'Contact information.';

SELECT ist_pk('phone');
SELECT ist_bk('phone', ARRAY['phone_type', 'phone_number']);
