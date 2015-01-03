alter table organizations add updated_by_guid uuid;
alter table organizations add updated_at timestamptz;
alter table organizations alter column updated_at set default now();

update organizations
   set updated_by_guid = created_by_guid,
       updated_at = created_at;

alter table organizations alter column updated_by_guid set not null;
alter table organizations alter column updated_at set not null;


select schema_evolution_manager.create_updated_at_trigger('public', 'organizations');
   
