update users
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and email like '%@test.apidoc.me';

update organizations
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and (key like 'a-public-%'
        or key like 'z-test-%');

update memberships
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and organization_guid in (select guid from organizations where deleted_at is not null);

update membership_requests
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and organization_guid in (select guid from organizations where deleted_at is not null);

update organization_domains
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and organization_guid in (select guid from organizations where deleted_at is not null);

update subscriptions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and organization_guid in (select guid from organizations where deleted_at is not null);

update applications
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and organization_guid in (select guid from organizations where deleted_at is not null);

update application_moves
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and application_guid in (select guid from applications where deleted_at is not null);

update attributes
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and name like 'z-test%';

update organization_attribute_values
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and value like 'z-test%';

update changes
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and application_guid in (select guid from applications where deleted_at is not null);

delete from search.items
 where application_guid in (select guid from applications where deleted_at is not null);

update versions
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and application_guid in (select guid from applications where deleted_at is not null);

update watches
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and application_guid in (select guid from applications where deleted_at is not null);

update generators.services
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and uri like 'http://test.generator.%';

update generators.generators
   set deleted_at=now(), deleted_by_guid = created_by_guid
 where deleted_at is null
   and service_guid in (select guid from generators.services where deleted_at is not null);
