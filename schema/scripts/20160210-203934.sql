drop table if exists values;
drop table if exists attributes;

create table attributes (
  guid                    uuid primary key,
  name                    text not null check(trim(name) = name) check(name != '') check(lower(name) = name),
  description             text
);

select schema_evolution_manager.create_basic_audit_data('public', 'attributes');

comment on table attributes is '
  User defined attributes that can be used to enhance code
  generation. The attribute name is globally unique - and is passed in
  to code generators during invocation.
';

create unique index attributes_name_not_deleted_un_idx on attributes(name) where deleted_at is null;

create table organization_attribute_values (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  attribute_guid          uuid not null references attributes,
  value                   text not null check(trim(value) = value) check(value != '')
);

select schema_evolution_manager.create_basic_audit_data('public', 'organization_attribute_values');

comment on table organization_attribute_values is '
  User defined organization_attribute_values that can be used to enhance code
  generation. The value value is globally unique - and is passed in
  to code generators during invocation.
';

create index on organization_attribute_values(organization_guid);
create index on organization_attribute_values(attribute_guid);

create unique index organization_attribute_values_organization_attribute_value_not_del_un_idx
    on organization_attribute_values(organization_guid, attribute_guid, value)
 where deleted_at is null;

