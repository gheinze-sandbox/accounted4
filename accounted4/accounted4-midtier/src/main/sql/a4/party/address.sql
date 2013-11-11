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
