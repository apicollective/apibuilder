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

You'll need to get the api schema into your dev postgresql database; to do that:

$ psql
psql> CREATE DATABASE api;

Then you'll need to create the database schema using [schema evolution manager](https://github.com/gilt/schema-evolution-manager#installation)

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
  sbt> run

Goto http://localhost:9000 in your browser

Now both should be running and able to talk to each other, and should recompile
in situ for a nice development experience.

Updating generated code
=======================
curl http://www.apidoc.me/gilt/code/api-doc/latest/ruby_client > client-tests/ruby_client.rb
curl http://www.apidoc.me/gilt/code/api-doc/latest/play_2_3_client > www/app/client/GeneratedClient.scala
curl http://www.apidoc.me/gilt/code/api-doc/latest/play_2_x_json > core/src/main/scala/core/Generated.scala
curl http://www.apidoc.me/gilt/code/api-doc/latest/play_2_x_routes > api/conf/routes

Releasing code
==============

Install ionblaster:
  curl -s https://s3.amazonaws.com/ionblaster/install | sh

Release to ec2:
  /web/metadata-architecture/exec script/release-and-deploy [--tag x.y.z] api www
