insert into applications
(guid, organization_guid, name, key, visibility, description,
 created_at, created_by_guid, updated_at, updated_by_guid, deleted_at, deleted_by_guid)
select guid, organization_guid, name, key, visibility, description,
       created_at, created_by_guid, updated_at, updated_by_guid, deleted_at, deleted_by_guid
  from services
 order by created_at;


update subscriptions set publication = 'applications.create' where publication = 'services.create';
