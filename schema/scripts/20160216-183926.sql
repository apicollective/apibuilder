alter table generators.services add attributes text;

comment on column generators.services.attributes is '
  A space separated list of the names of the attributes, if any, that
  this generator can use.
';
