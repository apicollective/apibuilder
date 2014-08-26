alter table services add visibility text check (visibility in ('public', 'organization'));
update services set visibility = 'organization';





