Swagger Support
===============

We aim to keep up with the [Swagger 2.0 Specification](http://swagger.io/specification/) for both import and export.
Because Apidoc is a subset of Swagger coverage, there will be some features that we consciously do not support; those
gaps will be noted here.


Intentional Gaps
================

1. Data types
    1. byte
    2. binary
2. Schema / Parameter Object
    1. multipleOf
    2. exclusiveMaximum
    3. exclusiveMinimum
    4. maxLength
    5. minLength
    6. pattern
    7. maxItems
    8. minItems
    9. uniqueItems
    10. maxProperties
    11. minProperties
    12. additionalProperties
    13. discriminator
    14. readOnly
    15. xml
    16. default (at the model level, because it can be compiled from the properties)
3. tags
4. top-level parameters
5. top-level responses
5. termsOfService
6. Operation object
    1. tags
    2. summary
    3. externalDocs
    4. operationId
    5. schemes
    6. deprecated
    7. security
7. Name of body parameter
8. externalDocs get appended to the description
9. non-JSON produces/consumes (for documentation only) at service & operation level
10. collectionFormat on array values


Gaps on the To-Do List
======================

1. Data types
    1. password


Apidoc Features not Supported by Swagger
========================================

1. Data types
    1. decimal (float?)
    2. object
    3. unit
    4. uuid
2. Overriding pluralization of model names
3. organization
4. application
5. namespace
6. Resource-level description
7. Parameter-level example


Internal To-Dos
===============

1. [imports](https://github.com/apicollective/apibuilder-swagger-generator/issues/3)
2. [More automated testing](https://github.com/apicollective/apibuilder-swagger-generator/issues/4)
3. [Integration tests](https://github.com/apicollective/apibuilder-swagger-generator/issues/5)
