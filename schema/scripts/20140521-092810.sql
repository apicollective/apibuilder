update users
   set email = 'admin@apidoc.me'
 where email = 'admin@apidoc.com'
   and deleted_at is null;

update services
   set deleted_at = now(),
       deleted_by_guid = (select guid from users where deleted_at is null and email='admin@apidoc.me')
 where deleted_at is null
   and created_at < '2014-05-21 10:27:07.047495+00'
   and name = 'Catalog Publisher';

update versions
   set deleted_at = now(),
       deleted_by_guid = (select guid from users where deleted_at is null and email='admin@apidoc.me')
 where deleted_at is null
   and created_at < '2014-05-21 10:27:07.047495+00';
