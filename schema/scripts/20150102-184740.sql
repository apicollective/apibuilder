-- namespace was not unique and turns out we don't want it to be
-- (users create their own accounts on behalf of an org and reuse the
-- same namespace).

-- create unique index on organizations(lower(trim(namespace))) where deleted_at is null;
