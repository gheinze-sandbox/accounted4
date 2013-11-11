CREATE TABLE email(
  email_type        VARCHAR(16)
 ,email_address     VARCHAR(64)  
 ,notes             TEXT
) INHERITS(base);

COMMENT ON TABLE email IS 'Contact information.';

SELECT ist_pk('email');
SELECT ist_bk('email', ARRAY['email_type', 'email_address']);
