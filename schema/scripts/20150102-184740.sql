create unique index on organizations(lower(trim(namespace))) where deleted_at is null;
