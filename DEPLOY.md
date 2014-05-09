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

    git tag -a -m 0.0.17 0.0.17
    git push --tags origin

    DOCKER_HOST=tcp://localhost:4244 /web/metadata-architecture/exec script/build-docker-image apidoc svc 0.0.17
    /web/metadata-architecture/exec script/deploy api.origin.apidoc.me apidoc svc 0.0.17
    /web/metadata-architecture/exec script/ionblaster set-load-balancers -lb <HOST> api.origin.apidoc.me

    DOCKER_HOST=tcp://localhost:4244 /web/metadata-architecture/exec script/build-docker-image apidoc web 0.0.17
    /web/metadata-architecture/exec script/deploy www.origin.apidoc.me apidoc web 0.0.17
    /web/metadata-architecture/exec script/ionblaster set-load-balancers -lb <HOST> www.origin.apidoc.me

Metadata about your app EC2 Configuration
=========================================

    /web/metadata-architecture/exec script/ionblaster stacks

Removing an old instance
========================

    /web/metadata-architecture/exec script/ionblaster delete-stack apidoc-svc-0-5-2-production-201412012928

Releasing a schema change
=========================

    cd /web/apidoc/schema
    sem-dist
    scp -F /web/metadata-architecture/ssh/config /web/schema-apidoc/dist/schema-apidoc-*.tar.gz dbgateway:~/
    ssh -F /web/metadata-architecture/ssh/config dbgateway
    tar xfz schema-apidoc-*.tar.gz
    cd schema-apidoc-0.0.31
    sem-apply --user web --host apidoc.cqe9ob8rnh0u.us-east-1.rds.amazonaws.com --name apidoc

Debugging
=========

 - Login to EC2 https://console.aws.amazon.com/ec2/v2/home?region=us-east-1#Instances:search=i-079f7c54

 - SSH to an instance
   ssh -F /web/metadata-architecture/ssh/config <EC2 Hostname>

 - sudo docker ps -a

 - sudo docker logs <container id>

 - Docker log location: /var/log/upstart/docker.log







