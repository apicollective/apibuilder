alter table applications add namespace text check (lower(trim(namespace)) = namespace);
create unique index on applications(namespace) where deleted_at is null;

update applications
   set namespace = 'com.' || (select key from organizations where guid = organization_guid) ||'.' || key
 where namespace is null;

alter table applications alter column namespace set not null;
