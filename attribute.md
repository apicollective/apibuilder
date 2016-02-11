Add support for extensible attributes that can be used by the code
generators to enhance the client libraries.

Initial use case is to support import paths for the go clients. The
pattern we are using here is:

  - we have created a github repository
    )(github.com/flowcommerce/apidoc where we commit the generated
    clients.

  - each individual client library goes into a subfolder with the name
    of the application. For example, an apidoc application will have
    its client code committed to:
    github.com/flowcommerce/apidoc/registry

  - our users can not "go get github.com/flowcommerce/apidoc", and
    more importantly import generated clients into their CLIs. This
    pattern seems consistent with the go ecosystem and is easy to use

Within apidoc, in order to support this use case, we need to capture
the value 'github.com/flowcommerce/apidoc'. We can then use that in
the generated code to generate a complete path to the application.

There have been other use cases raised for attributes, and so to
support both this use case and thinking of enabling future ones the
proposed design of the attribute system:

  - Top level resource named 'attribute'. Each attribute has:

    a. a globally unique, url friendly name
    b. an optional description

  - The ability set attribute values at the organization level

  - Adding the list of attribute name/value pairs to the invocation
    form used in code generation

  - Adding an optional list of attribute names to the code generation
    interface itself, allowing a code generator to declare what
    attributes it uses. This is intended to aid discovery.

    go_import_base_url:
      In the go_clients, the base URL at which we will find generated code. Will replace the organiation domain with this URL when generating the code

    attribute:
      name: go_import_base_url
      generators: ['go_1_5_client']

    flow:
      attribute: go_import_base_url
      value: "github.com/flowcommerce/apidoc"

    [
      { "name": "go_import_base_url", "value": "github.com/flowcommerce/apidoc" }
    ]

    ["io.flow" -> "github.com/flowcommerce/apidoc"]
    
