Deploying apidoc
================

 - Deployed to gilt-architecture EC2 subaccount

 - Credentials and information for the EC2 account in gerrit repo
   metadata-architecture

 - Runs in EC2 on docker images. Database is RDS Postgresql

Installing Docker on mac
========================

  http://docs.docker.io/installation/mac/

Deploying
==============

  /web/ionroller-tools/bin/release-and-deploy --release "apidoc-api apidoc-www"

Releasing a schema change
=========================

    sudo yum install http://yum.postgresql.org/9.4/redhat/rhel-6-x86_64/pgdg-ami201503-94-9.4-1.noarch.rpm
    sudo yum install postgresql94

    cd /web/apidoc/schema
    sem-dist
    scp -F /web/metadata-architecture/ssh/config /web/schema-apidoc/dist/schema-apidoc-*.tar.gz dbgateway:~/
    ssh -F /web/metadata-architecture/ssh/config dbgateway
    tar xfz schema-apidoc-*.tar.gz
    cd schema-apidoc-*

    echo "apidoc.prod.rds.gilt.local:5432:apidoc:api:PASSWORD" > ~/.pgpass
    chmod 0600 ~/.pgpass

    sem-apply --user apoi --host apidoc.prod.rds.gilt.local --name apidoc

Database backup
===============

    pg_dump  -Fc -h apidoc.prod.rds.gilt.local -U api -f apidoc.dmp apidoc
    pg_restore -U api -c -d apidoc apidoc.dmp

Debugging
=========

 - Login to EC2 https://console.aws.amazon.com/

 - SSH to an instance
   ssh -F /web/metadata-architecture/ssh/mbryzek <EC2 Hostname>

 - sudo docker ps -a

 - sudo docker logs <container id>

 - Docker log location: /var/log/upstart/docker.log
