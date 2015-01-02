alter table organizations add namespace text;

update organizations
   set namespace = (select lower(trim(package_name)) from organization_metadata where organization_guid = organizations.guid and deleted_at is null);

update organizations
   set namespace = 'com.' || lower(trim(name))
 where namespace is null
    or trim(namespace) = '';

alter table organizations alter column namespace set not null;
