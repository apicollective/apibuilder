alter table organizations add visibility text;

update organizations
   set visibility = (select visibility from organization_metadata where organization_guid = organizations.guid and deleted_at is null);

update organizations
   set visibility = 'organization'
 where visibility is null;

alter table organizations alter column visibility set not null;
