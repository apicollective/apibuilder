alter table organization_metadata add visibility text check (visibility in ('public', 'organization'));
update organization_metadata set visibility = 'organization';
