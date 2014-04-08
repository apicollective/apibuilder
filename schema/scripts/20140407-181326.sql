insert into users
(guid, email, created_by_guid, updated_by_guid)
values
('f3973f60-be9f-11e3-b1b6-0800200c9a66', 'admin@apidoc.com', 'f3973f60-be9f-11e3-b1b6-0800200c9a66', 'f3973f60-be9f-11e3-b1b6-0800200c9a66');

insert into tokens
(guid, user_guid, token, created_by_guid)
select '0faf0520-bea0-11e3-b1b6-0800200c9a66', guid, 'ZdRD61ODVPspeV8Wf18EmNuKNxUfjfROyJXtNJXj9GMMwrAxqi8I4aUtNAT6', guid
  from users
 where email = 'admin@apidoc.com';
