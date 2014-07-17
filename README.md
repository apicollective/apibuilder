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

## Scala Client

- In order to use the generated client code, place it in a .scala file at the
  root package.

- The client methods return Any and the expectation is that the caller will
  use pattern matching to dispatch on what was actually returned by the server.

- Currently path params are all generated as Strings, because there is not a
  way to attach type information to them from the api.json file.

Installing Docker on mac
========================

  http://docs.docker.io/installation/mac/

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
  sbt> project svc
  sbt> run 9001

...then in another screen, do:

  $ sbt
  sbt> project web
  sbt> run

Now both should be running and able to talk to each other, and should recompile
in situ for a nice development experience.

Releasing code
==============

    /web/metadata-architecture/exec /web/svc-iris-hub/script/release-and-deploy api [optional tag]
    /web/metadata-architecture/exec /web/svc-iris-hub/script/release-and-deploy www [optional tag]

Metadata about your app EC2 Configuration
=========================================

    ionblaster stacks
