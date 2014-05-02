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

- Proper string formatting for dates
- Better return types in generated clients
