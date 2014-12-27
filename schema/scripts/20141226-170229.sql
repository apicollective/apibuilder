alter table watches add application_guid uuid references applications;
update watches set application_guid = service_guid;
alter table watches alter application_guid set not null;
alter table watches drop service_guid;
create index on watches(application_guid);
