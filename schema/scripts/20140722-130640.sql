create table organization_domains (
  guid                    uuid primary key,
  organization_guid       uuid not null references organizations,
  domain                  text not null check (lower(trim(domain)) = domain)
);

select schema_evolution_manager.create_basic_created_audit_data('public', 'organization_domains');
select schema_evolution_manager.create_basic_deleted_audit_data('public', 'organization_domains');

create index on organization_domains(organization_guid);
create unique index organization_domain_domain_not_deleted_un_idx on organization_domains(domain) where deleted_at is null;

comment on table organization_domains is '
  Stores a list of domains that belong to this organization. The initial
  use case was to associate new users automatically with their organization.
  For example, if you register with a foo@gilt.com email address, we can
  look up the organization for the domain gilt.com and add you as a member
  of Gilt automatically (assuming verification by email or other).
';
  
comment on column organization_domains.domain is '
  e.g. gilt.com or giltcity.com. Required to be stored in lower case
  to facilitate lookup and uniqueness (domain names are case insensitive).
';
  

