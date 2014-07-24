apidoc
======

Designed to host API documentation for REST services. See api/api.json for
description of the API.

There are four SBT subprojects

core
====

Contains shared code to parse an api.json file into case classes,
including validation on the document in a way that is friendly to
users.

api
===
apidoc REST service itself. See api/api.json contains the description
of the API.

www
===
Actual UI for apidoc.

sbt-apigen
==========
SBT plugin which can generate client APIs from an API description, where
the JSON files are stored in src/main/api.

Developing
==========

You'll need to get the api schema into your dev postgresql database.

$ psql
psql> CREATE DATABASE api;

Then you'll need to create the database schema using schema evolution manager.  Installation instructions are [here](https://github.com/gilt/schema-evolution-manager#installation).

$ cd /web/apidoc/schema
$ ./dev.rb

The application consists of a service on port 9001, and a web app on port 9000.

One way to do this is to run a screen session, and in one screen do:

  $ sbt
  sbt> project api
  sbt> run 9001

...then in another screen, do:

  $ sbt
  sbt> project www
  sbt> run 9002

Now both should be running and able to talk to each other, and should recompile
in situ for a nice development experience.

Releasing code
==============

Install ionblaster:
  curl -s https://s3.amazonaws.com/ionblaster/install | sh

Release to ec2:
  /web/metadata-architecture/exec script/release-and-deploy [--tag x.y.z] api www
