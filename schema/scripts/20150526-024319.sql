alter table generators alter column uri set not null;
create unique index generators_key_not_deleted_un_idx on generators(key) where deleted_at is null;
