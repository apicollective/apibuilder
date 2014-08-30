create unique index on services(organization_guid, lower(trim(name))) where deleted_at is null;
