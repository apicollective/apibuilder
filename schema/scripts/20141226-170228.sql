alter table versions add application_guid uuid references applications;
update versions set application_guid = service_guid;
alter table versions alter application_guid set not null;
alter table versions drop service_guid;
create index on versions(application_guid);
