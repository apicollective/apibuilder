alter table generators.generators add attributes text;

comment on column generators.generators.attributes is '
  A space separated list of the names of the attributes, if any, that
  this generator can use.
';
