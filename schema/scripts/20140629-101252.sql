insert into user_passwords
(guid, user_guid, algorithm_key, hash, created_by_guid, updated_by_guid)
values
('d5f119dd-1e69-4a84-8b75-7bf73362c563',
 (select guid from users where deleted_at is null and email = 'admin@apidoc.me'),
 'bcrypt',
 'JDJhJDEwJDZkYm5venp1clNhWkpYM1ZUdWNIOWVMODh1ZzNpQUs3bmRodUd2TXQ1MjJzaC8wTWFLcXZD',
 (select guid from users where deleted_at is null and email = 'admin@apidoc.me'),
 (select guid from users where deleted_at is null and email = 'admin@apidoc.me')
);
