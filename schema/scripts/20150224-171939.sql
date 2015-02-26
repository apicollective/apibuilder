update users set nickname = guid where deleted_at is not null and nickname is null;

create or replace function dedup_nicknames_create_tables() returns void as $$
declare
  row   record;
  v_num integer;
  v_number_users_with_nickname integer;
begin
  execute 'drop table if exists tmp';
  execute 'create table tmp as ' ||
          ' select guid, created_at, substr(lower(email), 0, position(''@'' in email)) nick, 1 as num' ||
          '   from users' ||
          '  where nickname is null' ||
          '    and deleted_at is null';

  execute 'alter table tmp add primary key(guid)';
  execute 'alter table tmp alter column created_at set not null';
  execute 'alter table tmp alter column nick set not null';
  execute 'alter table tmp alter column num set not null';
  execute 'create index on tmp(nick)';

  execute 'drop table if exists dups';
  execute 'create table dups as select nick, count(*) as count, min(created_at) as min_created_at from tmp group by nick';
  execute 'create index on dups(nick)';
  execute 'update tmp set num = (select count from dups where dups.nick = tmp.nick) where nick in (select nick from dups where count > 1)';
end;
$$ language plpgsql;


create or replace function dedup_nicknames_single_pass() returns integer as $$
declare
  row   record;
  v_num integer;
  v_number_users_with_nickname integer;
begin
  perform dedup_nicknames_create_tables();

  for row in (select * from dups) loop
    select count(*) into v_number_users_with_nickname
      from users
     where users.deleted_at is null
       and (users.nickname = row.nick or users.nickname like row.nick || '-%');

    if v_number_users_with_nickname = 0 then
      update users
         set nickname = (select tmp.nick from tmp where tmp.guid = users.guid)
       where deleted_at is null
         and nickname is null
         and guid = (select tmp.guid from tmp where tmp.nick = row.nick and tmp.created_at = row.min_created_at);
    else
      update users
         set nickname = (select tmp.nick from tmp where tmp.guid = users.guid) || '-' || (v_number_users_with_nickname+1)
       where deleted_at is null
         and nickname is null
         and guid = (select tmp.guid from tmp where tmp.nick = row.nick and tmp.created_at = row.min_created_at);
    end if;
  end loop;

  select count(*) from dups into v_num;

  return v_num;
end;
$$ language plpgsql;

create or replace function dedup_nicknames() returns void as $$
begin
  while dedup_nicknames_single_pass() > 0 loop
  end loop;
end;
$$ language plpgsql;

select dedup_nicknames_create_tables();

update users
   set nickname = (select tmp.nick from tmp where tmp.guid = users.guid)
 where guid in (select guid from tmp where num = 1);

select dedup_nicknames();

alter table users alter column nickname set not null;

--drop function dedup_nicknames_single_pass();
--drop function dedup_nicknames_create_tables();
--drop function dedup_nicknames();
--drop table if exists tmp;
--drop table if exists dups;
