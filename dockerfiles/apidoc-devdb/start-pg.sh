#!/bin/bash

PGDATA=/var/lib/pgsql/data

if [ ! -f "$PGDATA/postgresql.conf" ] ; then
  echo "initing new apidoc-devdb"
  /usr/libexec/postgresql-ctl init
  sed -i "s/ident\|peer/trust/g" $PGDATA/pg_hba.conf
  echo "host    all             all             0.0.0.0/0            trust" >> $PGDATA/pg_hba.conf
  echo "listen_addresses = '*'" >> $PGDATA/postgresql.conf

  INITDB=true
fi;

rm -rf $PGDATA/postmaster.pid
/usr/libexec/postgresql-ctl start
sleep 10

if [ -n "$INITDB" ] ; then
  psql -c "CREATE DATABASE apidoc"
  psql -c "CREATE ROLE web WITH LOGIN; GRANT ALL ON DATABASE apidoc TO web;"
fi

