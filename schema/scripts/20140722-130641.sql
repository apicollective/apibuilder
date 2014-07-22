insert into organization_domains
(guid, organization_guid, domain, created_by_guid)
select '3622ca69-ef19-4508-83ea-abe6c53d4b64',
       guid,
       'gilt.com',
       created_by_guid
  from organizations
 where lower(trim(name)) = 'gilt'
   and deleted_at is null;

insert into organization_domains
(guid, organization_guid, domain, created_by_guid)
select '169daeaf-7e1a-4dbb-9d7c-551e811b4681',
       guid,
       'giltcity.com',
       created_by_guid
  from organizations
 where lower(trim(name)) = 'gilt'
   and deleted_at is null;

