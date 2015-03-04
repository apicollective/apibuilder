update users
   set deleted_at = now(),
       deleted_by_guid = (select guid from users where email='admin@apidoc.me' and deleted_at is null)
 where deleted_at is null
   and email in('jenglert@gitl.com', 'mbryzek-test@gilt.com', 'michael-public@gilt.com');

update users
   set email = 'nitay@actioniq.com'
 where email = 'nitay@actioniq.co';

