drop table if exists email_verification_confirmations;
drop table if exists email_verifications;

create table email_verifications (
  guid                    uuid not null primary key,
  user_guid               uuid not null references users,
  email                   text not null check (trim(email) = email),
  token                   text not null unique check (trim(token) = token),
  expires_at              timestamptz not null
);

select schema_evolution_manager.create_basic_audit_data('public', 'email_verifications');
alter table email_verifications drop column updated_by_guid; -- never updated

create index on email_verifications(user_guid);


comment on table email_verifications is '
  Tracks that we sent an email to verify an email address on behalf of
  a particular user.
';

comment on column email_verifications.email is '
  The actual email address to which we delivered the message. We
  record the email address here as it is possible for the user to
  change their email address AFTER we send the verification note.
';

comment on column email_verifications.token is '
  Unique token used to verify the email added.
';

comment on column email_verifications.expires_at is '
  The token is only valid for a short period of time. We record the
  expiration date w/ the record to ensure that if we communicate the
  expiration date to the user, it is recorded identically here.
';


create table email_verification_confirmations (
  guid                    uuid not null primary key,
  email_verification_guid uuid not null references email_verifications unique
);

select schema_evolution_manager.create_basic_audit_data('public', 'email_verification_confirmations');
alter table email_verification_confirmations drop column updated_by_guid; -- never updated

comment on table email_verification_confirmations is '
  Records that an email has been verified
';
