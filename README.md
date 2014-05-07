apidoc
======

Designed to host API documentation for REST services. See svc/api.json for
description of the API.

There are four SBT subprojects

core
====

Contains shared code to parse an api.json file into case classes,
including validation on the document in a way that is friendly to
users.

svc
===
apidoc REST service itself. See svc/api.json contains the description
of the API.

web
===
Actual UI for apidoc.

sbt-apigen
==========
SBT plugin which can generate client APIs from an API description, where
the JSON files are stored in src/main/api.

# Code Generation

## Scala

API doc supports generating a Play 2 routes file as well as a Play 2 client.

### TODO

- Better return types in generated clients

Installing Docker on mac
========================

  http://docs.docker.io/installation/mac/

Releasing code
==============

    git tag -a -m 0.0.13 0.0.13
    git push --tags origin
    DOCKER_HOST=tcp://localhost:4244 /web/metadata-architecture/exec script/build-docker-image apidoc web 0.0.13
    /web/metadata-architecture/exec script/deploy api.iris.gilt.com apidoc web 0.0.13

    DOCKER_HOST=tcp://localhost:4244 /web/metadata-architecture/exec script/build-docker-image apidoc svc 0.0.13
    /web/metadata-architecture/exec script/deploy api.iris.gilt.com apidoc svc 0.0.13

Putting traffic on your new version
===================================

    /web/metadata-architecture/exec script/ionblaster set-load-balancers -lb <HOST> api.iris.gilt.com

Metadata about your app EC2 Configuration
=========================================

    /web/metadata-architecture/exec script/ionblaster stacks
