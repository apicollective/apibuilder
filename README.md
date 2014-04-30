apidoc
======

Designed to host API documentation for REST services. See svc/api.json for
description of the API.

There are three SBT subprojects

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
