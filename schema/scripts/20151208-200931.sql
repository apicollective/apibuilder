create unique index generators_lower_key_un_idx on generators.generators(lower(key)) where deleted_at is null;
