update versions
   set deleted_at=now(),deleted_by_guid = (select guid from users where deleted_at is null and email='admin@apidoc.me')
 where deleted_at is null
   and version != (select v2.version from versions v2 where v2.deleted_at is null and v2.application_guid = versions.application_guid order by version_sort_key desc limit 1)
   and guid not in (select version_guid from cache.services where deleted_at is null and version = '0.8.18');
