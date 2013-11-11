-- Create an admin account "a4admin" with password "a42day"

insert into spring_security.users values('a4admin', '$2a$10$cJw2PP46Q20dRDz5aXlGb.lDSM.c2XtJMtoft2Y5xz.CINBdqN3ZC', true)
;
insert into spring_security.authorities values('a4admin', 'ROLE_USER')
;
insert into spring_security.authorities values('a4admin', 'ROLE_ADMIN')
;
