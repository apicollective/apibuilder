alter table users add nickname text check( lower(trim(nickname)) = nickname );
create unique index users_nickname_not_deleted_un_idx on users(nickname) where deleted_at is null;

