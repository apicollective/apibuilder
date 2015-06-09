create or replace function record_move(p_guid in uuid, p_application_key in varchar) returns void as $$
begin
  insert into application_moves
  (guid, application_guid, from_organization_guid, to_organization_guid, created_by_guid)
  values
  (
   p_guid,
   (select guid from applications where deleted_at is null and key = p_application_key),
   (select guid from organizations where deleted_at is null and key = 'gilt'),
   (select guid from organizations where deleted_at is null and key = 'bryzek'),
   coalesce(
     (select guid from users where deleted_at is null and email = 'michael@gilt.com'),
     coalesce(
       (select guid from users where deleted_at is null and email = 'mbryzek@alum.mit.edu'),
       (select guid from users where deleted_at is null and email = 'admin@apidoc.me')
     )
   )
  );
end
$$ language plpgsql;

select record_move('6d18f16c-6ce8-47c4-9899-b14a90d7f152'::uuid, 'apidoc-spec');
select record_move('d0bcace2-a340-484d-9991-0f17a8f0dd13'::uuid, 'apidoc-api');
select record_move('3167ae80-6918-4df8-b7ad-8ef466fb2ecd'::uuid, 'apidoc-example-union-types');
select record_move('b05c3182-3f85-4c9b-bbad-2b72055d058e'::uuid, 'apidoc-generator');
select record_move('947d5a73-7f1d-46c7-9d3a-9b087070fe13'::uuid, 'apidoc-internal');

drop function record_move(uuid, varchar);
