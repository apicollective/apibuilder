Deploying apibuilder
====================

 - Runs in EC2 on docker images. Database is RDS Postgresql
 - yum install docker
 - service docker start
 - docker pull flowcommerce/apibuilder:0.12.1
 - docker run ...

Installing Docker on mac
========================

  http://docs.docker.io/installation/mac/

Building the Docker Image
=========================

  Step 1: Using a small set of scripts that will tag the repo, update
  any markup to latest tag, and then create the docker image:
  
  /web/ionroller-tools/bin/release-and-deploy

  You can also build the docker image directly:

 docker build -t flowcommerce/apibuilder:0.12.1 .

Database backup
===============

    pg_dump  -Fc -h host -U api -f apibuilderdb.dmp apibuilderdb
    pg_restore -U api -c -d apibuilderdb apibuilderdb.dmp
