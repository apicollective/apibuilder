alter table versions add original text;
alter table versions add service json;
update versions set original = json::varchar;
alter table versions rename json to old_json;
alter table versions alter original set not null;
alter table versions alter old_json drop not null;

comment on column versions.original is '
  Stores the original contents uploaded by the user.
';

comment on column versions.service is '
  Stores the version converted to the service specification.
';
