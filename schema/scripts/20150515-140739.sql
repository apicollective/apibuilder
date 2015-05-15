create table tasks (
  guid                    uuid primary key,
  task                    json not null,
  number_attempts         smallint default 0 not null check (number_attempts >= 0),
  last_error              text
);

select schema_evolution_manager.create_basic_audit_data('public', 'tasks');

comment on table tasks is '
  Stores a list of tasks that need to be completed. Common tasks are
  things like: Update the search index, create a diff of the service
  and record all the changes, etc. Tasks are created transactionally,
  then dispatched to actors to implement. Each actor deletes the
  task once complete (by marking the deleted_at column). A sweeper
  actor will occassionally resubmit tasks that failed.
';

comment on column tasks.task is '
  A task is defined at http://www.apidoc.me/gilt/apidoc-internal/latest#model-task
';

comment on column tasks.number_attempts is '
  Records the number of times we have attempted to run this task.
  Commonly we increment number attempts, process the task, and if
  succeeds we then delete the task. If it fails, we update last_error.
  This allows us to retry a task say twice; after which we no longer
  process the task (can notify an admin of the error).
';
