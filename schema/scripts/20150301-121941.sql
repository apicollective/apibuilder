insert into originals
(version_guid, type, data, created_at, created_by_guid, deleted_at, deleted_by_guid)
select guid, 'api_json', original, created_at, created_by_guid, deleted_at, deleted_by_guid
  from versions
 where original is not null
   and trim(original) != '{}'
   and trim(original) != '{ }';

