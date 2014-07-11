drop index memberships_organization_guid_user_guid_idx;

create unique index memberships_organization_guid_user_guid_role_idx on memberships(organization_guid, user_guid, role) WHERE deleted_at IS NULL;
