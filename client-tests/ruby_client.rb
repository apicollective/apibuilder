require 'cgi'
require 'net/http'
require 'net/https'
require 'uri'
require 'base64'

require 'rubygems'
require 'json'
require 'bigdecimal'

# Host API documentation for REST services, facilitating the design of good
# resource first APIs.
module Apidoc

  class Client

    USER_AGENT = 'apidoc:0.7.17 http://www.apidoc.me/gilt/code/apidoc/0.7.19/ruby_client' unless defined?(USER_AGENT)

    def initialize(url, opts={})
      @url = HttpClient::Preconditions.assert_class('url', url, String)
      @authorization = HttpClient::Preconditions.assert_class_or_nil('authorization', opts.delete(:authorization), HttpClient::Authorization)
      HttpClient::Preconditions.assert_empty_opts(opts)
      HttpClient::Preconditions.check_state(url.match(/http.+/i), "URL[%s] must start with http" % url)
    end

    def request(path=nil)
      HttpClient::Preconditions.assert_class_or_nil('path', path, String)
      request = HttpClient::Request.new(URI.parse(@url + path.to_s)).with_header('User-Agent', USER_AGENT)

      if @authorization
        request.with_auth(@authorization)
      else
        request
      end
    end

    def code
      @code ||= Apidoc::Clients::Code.new(self)
    end

    def domains
      @domains ||= Apidoc::Clients::Domains.new(self)
    end

    def generators
      @generators ||= Apidoc::Clients::Generators.new(self)
    end

    def healthchecks
      @healthchecks ||= Apidoc::Clients::Healthchecks.new(self)
    end

    def membership_requests
      @membership_requests ||= Apidoc::Clients::MembershipRequests.new(self)
    end

    def memberships
      @memberships ||= Apidoc::Clients::Memberships.new(self)
    end

    def organization_metadata
      @organization_metadata ||= Apidoc::Clients::OrganizationMetadata.new(self)
    end

    def organizations
      @organizations ||= Apidoc::Clients::Organizations.new(self)
    end

    def services
      @services ||= Apidoc::Clients::Services.new(self)
    end

    def subscriptions
      @subscriptions ||= Apidoc::Clients::Subscriptions.new(self)
    end

    def users
      @users ||= Apidoc::Clients::Users.new(self)
    end

    def validations
      @validations ||= Apidoc::Clients::Validations.new(self)
    end

    def versions
      @versions ||= Apidoc::Clients::Versions.new(self)
    end
  end

  module Clients

    class Code

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Generate code for a specific version of a service.
      def get_by_org_key_and_service_key_and_version_and_generator_key(org_key, service_key, version, generator_key)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        HttpClient::Preconditions.assert_class('version', version, String)
        HttpClient::Preconditions.assert_class('generator_key', generator_key, String)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}/#{CGI.escape(version)}/#{CGI.escape(generator_key)}").get { |hash| Apidoc::Models::Code.new(hash) }
      end

    end

    class Domains

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Add a domain to this organization
      def post(org_key, domain)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('domain', domain, Apidoc::Models::Domain)
        @client.request("/domains/#{CGI.escape(org_key)}").with_json(domain.to_json).post { |hash| Apidoc::Models::Domain.new(hash) }
      end

      # Remove this domain from this organization
      def delete_by_name(org_key, name)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('name', name, String)
        @client.request("/domains/#{CGI.escape(org_key)}/#{CGI.escape(name)}").delete
        nil
      end

    end

    class Generators

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # List all generators visible by this user
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :guid => HttpClient::Preconditions.assert_class_or_nil('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String),
          :key => HttpClient::Preconditions.assert_class_or_nil('key', opts.delete(:key), String),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/generators").with_query(query).get.map { |hash| Apidoc::Models::Generator.new(hash) }
      end

      def get_by_key(key)
        HttpClient::Preconditions.assert_class('key', key, String)
        @client.request("/generators/#{CGI.escape(key)}").get { |hash| Apidoc::Models::Generator.new(hash) }
      end

      def post(generator_create_form)
        HttpClient::Preconditions.assert_class('generator_create_form', generator_create_form, Apidoc::Models::GeneratorCreateForm)
        @client.request("/generators").with_json(generator_create_form.to_json).post { |hash| Apidoc::Models::Generator.new(hash) }
      end

      def put_by_key(key, generator_update_form)
        HttpClient::Preconditions.assert_class('key', key, String)
        HttpClient::Preconditions.assert_class('generator_update_form', generator_update_form, Apidoc::Models::GeneratorUpdateForm)
        @client.request("/generators/#{CGI.escape(key)}").with_json(generator_update_form.to_json).put { |hash| Apidoc::Models::Generator.new(hash) }
      end

      # Deletes a generator.
      def delete_by_key(key)
        HttpClient::Preconditions.assert_class('key', key, String)
        @client.request("/generators/#{CGI.escape(key)}").delete
        nil
      end

    end

    class Healthchecks

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      def get
        @client.request("/_internal_/healthcheck").get { |hash| Apidoc::Models::Healthcheck.new(hash) }
      end

    end

    class MembershipRequests

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search all membership requests. Results are always paginated.
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :org_guid => HttpClient::Preconditions.assert_class_or_nil('org_guid', HttpClient::Helper.to_uuid(opts.delete(:org_guid)), String),
          :org_key => HttpClient::Preconditions.assert_class_or_nil('org_key', opts.delete(:org_key), String),
          :user_guid => HttpClient::Preconditions.assert_class_or_nil('user_guid', HttpClient::Helper.to_uuid(opts.delete(:user_guid)), String),
          :role => HttpClient::Preconditions.assert_class_or_nil('role', opts.delete(:role), String),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/membership_requests").with_query(query).get.map { |hash| Apidoc::Models::MembershipRequest.new(hash) }
      end

      # Create a membership request
      def post(hash)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/membership_requests").with_json(hash.to_json).post { |hash| Apidoc::Models::MembershipRequest.new(hash) }
      end

      # Accepts this membership request. User will become a member of the
      # specified organization.
      def post_accept_by_guid(guid, hash)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/membership_requests/#{guid}/accept").with_json(hash.to_json).post
        nil
      end

      # Declines this membership request. User will NOT become a member of the
      # specified organization.
      def post_decline_by_guid(guid, hash)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/membership_requests/#{guid}/decline").with_json(hash.to_json).post
        nil
      end

    end

    class Memberships

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search all memberships. Results are always paginated.
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :org_guid => HttpClient::Preconditions.assert_class_or_nil('org_guid', HttpClient::Helper.to_uuid(opts.delete(:org_guid)), String),
          :org_key => HttpClient::Preconditions.assert_class_or_nil('org_key', opts.delete(:org_key), String),
          :user_guid => HttpClient::Preconditions.assert_class_or_nil('user_guid', HttpClient::Helper.to_uuid(opts.delete(:user_guid)), String),
          :role => HttpClient::Preconditions.assert_class_or_nil('role', opts.delete(:role), String),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/memberships").with_query(query).get.map { |hash| Apidoc::Models::Membership.new(hash) }
      end

      def get_by_guid(guid)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        @client.request("/memberships/#{guid}").get { |hash| Apidoc::Models::Membership.new(hash) }
      end

      def delete_by_guid(guid)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        @client.request("/memberships/#{guid}").delete
        nil
      end

    end

    class OrganizationMetadata

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Update metadata for this organization
      def put(key, organization_metadata)
        HttpClient::Preconditions.assert_class('key', key, String)
        HttpClient::Preconditions.assert_class('organization_metadata', organization_metadata, Apidoc::Models::OrganizationMetadata)
        @client.request("/organizations/#{CGI.escape(key)}/metadata").with_json(organization_metadata.to_json).put { |hash| Apidoc::Models::OrganizationMetadata.new(hash) }
      end

    end

    class Organizations

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search all organizations. Results are always paginated.
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :guid => HttpClient::Preconditions.assert_class_or_nil('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String),
          :user_guid => HttpClient::Preconditions.assert_class_or_nil('user_guid', HttpClient::Helper.to_uuid(opts.delete(:user_guid)), String),
          :key => HttpClient::Preconditions.assert_class_or_nil('key', opts.delete(:key), String),
          :name => HttpClient::Preconditions.assert_class_or_nil('name', opts.delete(:name), String),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/organizations").with_query(query).get.map { |hash| Apidoc::Models::Organization.new(hash) }
      end

      # Returns the organization with this key.
      def get_by_key(key)
        HttpClient::Preconditions.assert_class('key', key, String)
        @client.request("/organizations/#{CGI.escape(key)}").get { |hash| Apidoc::Models::Organization.new(hash) }
      end

      # Create a new organization.
      def post(hash)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/organizations").with_json(hash.to_json).post { |hash| Apidoc::Models::Organization.new(hash) }
      end

      # Deletes an organization and all of its associated services.
      def delete_by_key(key)
        HttpClient::Preconditions.assert_class('key', key, String)
        @client.request("/organizations/#{CGI.escape(key)}").delete
        nil
      end

    end

    class Services

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search all services. Results are always paginated.
      def get_by_org_key(org_key, incoming={})
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :name => HttpClient::Preconditions.assert_class_or_nil('name', opts.delete(:name), String),
          :key => HttpClient::Preconditions.assert_class_or_nil('key', opts.delete(:key), String),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/#{CGI.escape(org_key)}").with_query(query).get.map { |hash| Apidoc::Models::Service.new(hash) }
      end

      # Updates a service.
      def put_by_org_key_and_service_key(org_key, service_key, hash)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}").with_json(hash.to_json).put
        nil
      end

      # Deletes a specific service and its associated versions.
      def delete_by_org_key_and_service_key(org_key, service_key)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}").delete
        nil
      end

    end

    class Subscriptions

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search for a specific subscription.
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :id => HttpClient::Preconditions.assert_class_or_nil('id', opts.delete(:id), Integer),
          :organization_key => HttpClient::Preconditions.assert_class_or_nil('organization_key', opts.delete(:organization_key), String),
          :user_guid => HttpClient::Preconditions.assert_class_or_nil('user_guid', HttpClient::Helper.to_uuid(opts.delete(:user_guid)), String),
          :publication => HttpClient::Preconditions.assert_class_or_nil('publication', opts[:publication].nil? ? nil : (opts[:publication].is_a?(Apidoc::Models::Publication) ? opts.delete(:publication) : Apidoc::Models::Publication.apply(opts.delete(:publication))), Apidoc::Models::Publication),
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/subscriptions").with_query(query).get.map { |hash| Apidoc::Models::Subscription.new(hash) }
      end

      # Returns information about this subscription.
      def get_by_id(id)
        HttpClient::Preconditions.assert_class('id', id, String)
        @client.request("/subscriptions/#{CGI.escape(id)}").get { |hash| Apidoc::Models::Subscription.new(hash) }
      end

      # Create a new subscription.
      def post(subscription_form)
        HttpClient::Preconditions.assert_class('subscription_form', subscription_form, Apidoc::Models::SubscriptionForm)
        @client.request("/subscriptions").with_json(subscription_form.to_json).post { |hash| Apidoc::Models::Subscription.new(hash) }
      end

      def delete_by_id(id)
        HttpClient::Preconditions.assert_class('id', id, String)
        @client.request("/subscriptions/#{CGI.escape(id)}").delete
        nil
      end

    end

    class Users

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search for a specific user. You must specify at least 1 parameter -
      # either a guid, email or token - and will receive back either 0 or 1
      # users.
      def get(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :guid => HttpClient::Preconditions.assert_class_or_nil('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String),
          :email => HttpClient::Preconditions.assert_class_or_nil('email', opts.delete(:email), String),
          :token => HttpClient::Preconditions.assert_class_or_nil('token', opts.delete(:token), String)
        }.delete_if { |k, v| v.nil? }
        @client.request("/users").with_query(query).get.map { |hash| Apidoc::Models::User.new(hash) }
      end

      # Returns information about the user with this guid.
      def get_by_guid(guid)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        @client.request("/users/#{guid}").get { |hash| Apidoc::Models::User.new(hash) }
      end

      # Used to authenticate a user with an email address and password.
      # Successful authentication returns an instance of the user model. Failed
      # authorizations of any kind are returned as a generic error with code
      # user_authorization_failed.
      def post_authenticate(hash)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/users/authenticate").with_json(hash.to_json).post { |hash| Apidoc::Models::User.new(hash) }
      end

      # Create a new user.
      def post(hash)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/users").with_json(hash.to_json).post { |hash| Apidoc::Models::User.new(hash) }
      end

      # Updates information about the user with the specified guid.
      def put_by_guid(guid, hash)
        HttpClient::Preconditions.assert_class('guid', guid, String)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/users/#{guid}").with_json(hash.to_json).put { |hash| Apidoc::Models::User.new(hash) }
      end

    end

    class Validations

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      def post(value)
        HttpClient::Preconditions.assert_class('value', value, String)
        @client.request("/validations").with_body(value).post { |hash| Apidoc::Models::Validation.new(hash) }
      end

    end

    class Versions

      def initialize(client)
        @client = HttpClient::Preconditions.assert_class('client', client, Apidoc::Client)
      end

      # Search all versions of this service. Results are always paginated.
      def get_by_org_key_and_service_key(org_key, service_key, incoming={})
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        opts = HttpClient::Helper.symbolize_keys(incoming)
        query = {
          :limit => HttpClient::Preconditions.assert_class_or_nil('limit', opts.delete(:limit), Integer),
          :offset => HttpClient::Preconditions.assert_class_or_nil('offset', opts.delete(:offset), Integer)
        }.delete_if { |k, v| v.nil? }
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}").with_query(query).get.map { |hash| Apidoc::Models::Version.new(hash) }
      end

      # Retrieve a specific version of a service.
      def get_by_org_key_and_service_key_and_version(org_key, service_key, version)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        HttpClient::Preconditions.assert_class('version', version, String)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}/#{CGI.escape(version)}").get { |hash| Apidoc::Models::Version.new(hash) }
      end

      # Create or update the service with the specified version.
      def put_by_org_key_and_service_key_and_version(org_key, service_key, version, hash)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        HttpClient::Preconditions.assert_class('version', version, String)
        HttpClient::Preconditions.assert_class('hash', hash, Hash)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}/#{CGI.escape(version)}").with_json(hash.to_json).put { |hash| Apidoc::Models::Version.new(hash) }
      end

      # Deletes a specific version.
      def delete_by_org_key_and_service_key_and_version(org_key, service_key, version)
        HttpClient::Preconditions.assert_class('org_key', org_key, String)
        HttpClient::Preconditions.assert_class('service_key', service_key, String)
        HttpClient::Preconditions.assert_class('version', version, String)
        @client.request("/#{CGI.escape(org_key)}/#{CGI.escape(service_key)}/#{CGI.escape(version)}").delete
        nil
      end

    end

  end

  module Models
    class Publication

      attr_reader :value

      def initialize(value)
        @value = HttpClient::Preconditions.assert_class('value', value, String)
      end

      # Returns the instance of Publication for this value, creating a new instance for an unknown value
      def Publication.apply(value)
        if value.instance_of?(Publication)
          value
        else
          HttpClient::Preconditions.assert_class_or_nil('value', value, String)
          value.nil? ? nil : (from_string(value) || Publication.new(value))
        end
      end

      # Returns the instance of Publication for this value, or nil if not found
      def Publication.from_string(value)
        HttpClient::Preconditions.assert_class('value', value, String)
        Publication.ALL.find { |v| v.value == value }
      end

      def Publication.ALL
        @@all ||= [Publication.membership_requests.create, Publication.memberships.create, Publication.services.create, Publication.versions.create]
      end

      # For organizations for which I am an administrator, email me whenever a user
      # applies to join the org.
      def Publication.membership_requests.create
        @@_membership_requests.create ||= Publication.new('membership_requests.create')
      end

      # For organizations for which I am a member, email me whenever a user join the
      # org.
      def Publication.memberships.create
        @@_memberships.create ||= Publication.new('memberships.create')
      end

      # For organizations for which I am a member, email me whenever a service is
      # created.
      def Publication.services.create
        @@_services.create ||= Publication.new('services.create')
      end

      # For services that I watch, email me whenever a version is created.
      def Publication.versions.create
        @@_versions.create ||= Publication.new('versions.create')
      end

    end

    class Visibility

      attr_reader :value

      def initialize(value)
        @value = HttpClient::Preconditions.assert_class('value', value, String)
      end

      # Returns the instance of Visibility for this value, creating a new instance for an unknown value
      def Visibility.apply(value)
        if value.instance_of?(Visibility)
          value
        else
          HttpClient::Preconditions.assert_class_or_nil('value', value, String)
          value.nil? ? nil : (from_string(value) || Visibility.new(value))
        end
      end

      # Returns the instance of Visibility for this value, or nil if not found
      def Visibility.from_string(value)
        HttpClient::Preconditions.assert_class('value', value, String)
        Visibility.ALL.find { |v| v.value == value }
      end

      def Visibility.ALL
        @@all ||= [Visibility.user, Visibility.organization, Visibility.public]
      end

      # Only the creator can view this service
      def Visibility.user
        @@_user ||= Visibility.new('user')
      end

      # Any member of the organization can view this service
      def Visibility.organization
        @@_organization ||= Visibility.new('organization')
      end

      # Anybody, including non logged in users, can view this service
      def Visibility.public
        @@_public ||= Visibility.new('public')
      end

    end

    # Generated source code.
    class Code

      attr_reader :generator, :source

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @generator = HttpClient::Preconditions.assert_class('generator', opts[:generator].nil? ? nil : (opts[:generator].is_a?(Apidoc::Models::Generator) ? opts.delete(:generator) : Apidoc::Models::Generator.new(opts.delete(:generator))), Apidoc::Models::Generator)
        @source = HttpClient::Preconditions.assert_class('source', opts.delete(:source), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Code.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :generator => generator.nil? ? nil : generator.to_hash,
          :source => source
        }
      end

    end

    # Represents a single domain name (e.g. www.apidoc.me). When a new user
    # registers and confirms their email, we automatically associate that user with
    # a member of the organization associated with their domain. For example, if you
    # confirm your account with an email address of foo@gilt.com, we will
    # automatically create a membership request on your behalf to join the
    # organization with domain gilt.com.
    class Domain

      attr_reader :name

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @name = HttpClient::Preconditions.assert_class('name', opts.delete(:name), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Domain.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :name => name
        }
      end

    end

    class Error

      attr_reader :code, :message

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @code = HttpClient::Preconditions.assert_class('code', opts.delete(:code), String)
        @message = HttpClient::Preconditions.assert_class('message', opts.delete(:message), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Error.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :code => code,
          :message => message
        }
      end

    end

    # An apidoc generator
    class Generator

      attr_reader :guid, :key, :uri, :name, :language, :description, :visibility, :owner, :enabled

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @key = HttpClient::Preconditions.assert_class('key', opts.delete(:key), String)
        @uri = HttpClient::Preconditions.assert_class('uri', opts.delete(:uri), String)
        @name = HttpClient::Preconditions.assert_class('name', opts.delete(:name), String)
        @language = HttpClient::Preconditions.assert_class_or_nil('language', opts.delete(:language), String)
        @description = HttpClient::Preconditions.assert_class_or_nil('description', opts.delete(:description), String)
        @visibility = HttpClient::Preconditions.assert_class('visibility', opts[:visibility].nil? ? nil : (opts[:visibility].is_a?(Apidoc::Models::Visibility) ? opts.delete(:visibility) : Apidoc::Models::Visibility.apply(opts.delete(:visibility))), Apidoc::Models::Visibility)
        @owner = HttpClient::Preconditions.assert_class('owner', opts[:owner].nil? ? nil : (opts[:owner].is_a?(Apidoc::Models::User) ? opts.delete(:owner) : Apidoc::Models::User.new(opts.delete(:owner))), Apidoc::Models::User)
        @enabled = HttpClient::Preconditions.assert_boolean('enabled', opts.delete(:enabled))
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Generator.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :key => key,
          :uri => uri,
          :name => name,
          :language => language,
          :description => description,
          :visibility => visibility.nil? ? nil : visibility.value,
          :owner => owner.nil? ? nil : owner.to_hash,
          :enabled => enabled
        }
      end

    end

    # Form to create a new generator
    class GeneratorCreateForm

      attr_reader :key, :uri, :visibility

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @key = HttpClient::Preconditions.assert_class('key', opts.delete(:key), String)
        @uri = HttpClient::Preconditions.assert_class('uri', opts.delete(:uri), String)
        @visibility = HttpClient::Preconditions.assert_class('visibility', opts[:visibility].nil? ? nil : (opts[:visibility].is_a?(Apidoc::Models::Visibility) ? opts.delete(:visibility) : Apidoc::Models::Visibility.apply(opts.delete(:visibility))), Apidoc::Models::Visibility)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        GeneratorCreateForm.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :key => key,
          :uri => uri,
          :visibility => visibility.nil? ? nil : visibility.value
        }
      end

    end

    # Form to enable or disable a generator for an organization
    class GeneratorOrgForm

      attr_reader :enabled

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @enabled = HttpClient::Preconditions.assert_boolean('enabled', opts.delete(:enabled))
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        GeneratorOrgForm.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :enabled => enabled
        }
      end

    end

    # Form to update a generator
    class GeneratorUpdateForm

      attr_reader :visibility, :enabled

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @visibility = HttpClient::Preconditions.assert_class_or_nil('visibility', opts[:visibility].nil? ? nil : (opts[:visibility].is_a?(Apidoc::Models::Visibility) ? opts.delete(:visibility) : Apidoc::Models::Visibility.apply(opts.delete(:visibility))), Apidoc::Models::Visibility)
        @enabled = HttpClient::Preconditions.assert_boolean_or_nil('enabled', opts.delete(:enabled))
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        GeneratorUpdateForm.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :visibility => visibility.nil? ? nil : visibility.value,
          :enabled => enabled
        }
      end

    end

    class Healthcheck

      attr_reader :status

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @status = HttpClient::Preconditions.assert_class('status', opts.delete(:status), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Healthcheck.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :status => status
        }
      end

    end

    # A membership represents a user in a specific role to an organization.
    # Memberships cannot be created directly. Instead you first create a membership
    # request, then that request is either accepted or declined.
    class Membership

      attr_reader :guid, :user, :organization, :role

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @user = HttpClient::Preconditions.assert_class('user', opts[:user].nil? ? nil : (opts[:user].is_a?(Apidoc::Models::User) ? opts.delete(:user) : Apidoc::Models::User.new(opts.delete(:user))), Apidoc::Models::User)
        @organization = HttpClient::Preconditions.assert_class('organization', opts[:organization].nil? ? nil : (opts[:organization].is_a?(Apidoc::Models::Organization) ? opts.delete(:organization) : Apidoc::Models::Organization.new(opts.delete(:organization))), Apidoc::Models::Organization)
        @role = HttpClient::Preconditions.assert_class('role', opts.delete(:role), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Membership.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :user => user.nil? ? nil : user.to_hash,
          :organization => organization.nil? ? nil : organization.to_hash,
          :role => role
        }
      end

    end

    # A membership request represents a user requesting to join an organization with
    # a specific role (e.g. as a member or an admin). Membership requests can be
    # reviewed by any current admin of the organization who can either accept or
    # decline the request.
    class MembershipRequest

      attr_reader :guid, :user, :organization, :role

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @user = HttpClient::Preconditions.assert_class('user', opts[:user].nil? ? nil : (opts[:user].is_a?(Apidoc::Models::User) ? opts.delete(:user) : Apidoc::Models::User.new(opts.delete(:user))), Apidoc::Models::User)
        @organization = HttpClient::Preconditions.assert_class('organization', opts[:organization].nil? ? nil : (opts[:organization].is_a?(Apidoc::Models::Organization) ? opts.delete(:organization) : Apidoc::Models::Organization.new(opts.delete(:organization))), Apidoc::Models::Organization)
        @role = HttpClient::Preconditions.assert_class('role', opts.delete(:role), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        MembershipRequest.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :user => user.nil? ? nil : user.to_hash,
          :organization => organization.nil? ? nil : organization.to_hash,
          :role => role
        }
      end

    end

    # An organization is used to group a set of services together.
    class Organization

      attr_reader :guid, :key, :name, :domains, :metadata

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @key = HttpClient::Preconditions.assert_class('key', opts.delete(:key), String)
        @name = HttpClient::Preconditions.assert_class('name', opts.delete(:name), String)
        @domains = (opts.delete(:domains) || []).map { |el| el.nil? ? nil : (el.is_a?(Apidoc::Models::Domain) ? el : Apidoc::Models::Domain.new(el)) }
        @metadata = HttpClient::Preconditions.assert_class_or_nil('metadata', opts[:metadata].nil? ? nil : (opts[:metadata].is_a?(Apidoc::Models::OrganizationMetadata) ? opts.delete(:metadata) : Apidoc::Models::OrganizationMetadata.new(opts.delete(:metadata))), Apidoc::Models::OrganizationMetadata)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Organization.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :key => key,
          :name => name,
          :domains => (domains || []).map(&:to_hash),
          :metadata => metadata.nil? ? nil : metadata.to_hash
        }
      end

    end

    # Supplemental (non-required) information about an organization
    class OrganizationMetadata

      attr_reader :visibility, :package_name

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @visibility = HttpClient::Preconditions.assert_class_or_nil('visibility', opts[:visibility].nil? ? nil : (opts[:visibility].is_a?(Apidoc::Models::Visibility) ? opts.delete(:visibility) : Apidoc::Models::Visibility.apply(opts.delete(:visibility))), Apidoc::Models::Visibility)
        @package_name = HttpClient::Preconditions.assert_class_or_nil('package_name', opts.delete(:package_name), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        OrganizationMetadata.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :visibility => visibility.nil? ? nil : visibility.value,
          :package_name => package_name
        }
      end

    end

    # A service has a name and multiple versions of an API (Interface).
    class Service

      attr_reader :guid, :name, :key, :visibility, :description

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @name = HttpClient::Preconditions.assert_class('name', opts.delete(:name), String)
        @key = HttpClient::Preconditions.assert_class('key', opts.delete(:key), String)
        @visibility = HttpClient::Preconditions.assert_class('visibility', opts[:visibility].nil? ? nil : (opts[:visibility].is_a?(Apidoc::Models::Visibility) ? opts.delete(:visibility) : Apidoc::Models::Visibility.apply(opts.delete(:visibility))), Apidoc::Models::Visibility)
        @description = HttpClient::Preconditions.assert_class_or_nil('description', opts.delete(:description), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Service.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :name => name,
          :key => key,
          :visibility => visibility.nil? ? nil : visibility.value,
          :description => description
        }
      end

    end

    # Represents a user that is currently subscribed to a publication
    class Subscription

      attr_reader :guid, :organization, :user, :publication

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @organization = HttpClient::Preconditions.assert_class('organization', opts[:organization].nil? ? nil : (opts[:organization].is_a?(Apidoc::Models::Organization) ? opts.delete(:organization) : Apidoc::Models::Organization.new(opts.delete(:organization))), Apidoc::Models::Organization)
        @user = HttpClient::Preconditions.assert_class('user', opts[:user].nil? ? nil : (opts[:user].is_a?(Apidoc::Models::User) ? opts.delete(:user) : Apidoc::Models::User.new(opts.delete(:user))), Apidoc::Models::User)
        @publication = HttpClient::Preconditions.assert_class('publication', opts[:publication].nil? ? nil : (opts[:publication].is_a?(Apidoc::Models::Publication) ? opts.delete(:publication) : Apidoc::Models::Publication.apply(opts.delete(:publication))), Apidoc::Models::Publication)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Subscription.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :organization => organization.nil? ? nil : organization.to_hash,
          :user => user.nil? ? nil : user.to_hash,
          :publication => publication.nil? ? nil : publication.value
        }
      end

    end

    class SubscriptionForm

      attr_reader :organization_key, :user_guid, :publication

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @organization_key = HttpClient::Preconditions.assert_class('organization_key', opts.delete(:organization_key), String)
        @user_guid = HttpClient::Preconditions.assert_class('user_guid', HttpClient::Helper.to_uuid(opts.delete(:user_guid)), String)
        @publication = HttpClient::Preconditions.assert_class('publication', opts[:publication].nil? ? nil : (opts[:publication].is_a?(Apidoc::Models::Publication) ? opts.delete(:publication) : Apidoc::Models::Publication.apply(opts.delete(:publication))), Apidoc::Models::Publication)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        SubscriptionForm.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :organization_key => organization_key,
          :user_guid => user_guid,
          :publication => publication.nil? ? nil : publication.value
        }
      end

    end

    # A user is a top level person interacting with the api doc server.
    class User

      attr_reader :guid, :email, :name

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @email = HttpClient::Preconditions.assert_class('email', opts.delete(:email), String)
        @name = HttpClient::Preconditions.assert_class_or_nil('name', opts.delete(:name), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        User.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :email => email,
          :name => name
        }
      end

    end

    # Used only to validate json files - used as a resource where http status code
    # defines success
    class Validation

      attr_reader :valid, :errors

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @valid = HttpClient::Preconditions.assert_boolean('valid', opts.delete(:valid))
        @errors = (opts.delete(:errors) || []).map { |v| HttpClient::Preconditions.assert_class_or_nil('errors', v, String)}
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Validation.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :valid => valid,
          :errors => errors
        }
      end

    end

    # Represents a unique version of the service.
    class Version

      attr_reader :guid, :version, :json

      def initialize(incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @guid = HttpClient::Preconditions.assert_class('guid', HttpClient::Helper.to_uuid(opts.delete(:guid)), String)
        @version = HttpClient::Preconditions.assert_class('version', opts.delete(:version), String)
        @json = HttpClient::Preconditions.assert_class('json', opts.delete(:json), String)
      end

      def to_json
        JSON.dump(to_hash)
      end

      def copy(incoming={})
        Version.new(to_hash.merge(HttpClient::Helper.symbolize_keys(incoming)))
      end

      def to_hash
        {
          :guid => guid,
          :version => version,
          :json => json
        }
      end

    end

  end

  # ===== END OF SERVICE DEFINITION =====
  module HttpClient

    class Request

      def initialize(uri)
        @uri = Preconditions.assert_class('uri', uri, URI)
        @params = nil
        @body = nil
        @auth = nil
        @headers = {}
        @header_keys_lower_case = []
      end

      def with_header(name, value)
        Preconditions.check_not_blank('name', name, "Header name is required")
        Preconditions.check_not_blank('value', value, "Header value is required")
        Preconditions.check_state(!@headers.has_key?(name),
                                  "Duplicate header named[%s]" % name)
        @headers[name] = value
        @header_keys_lower_case << name.downcase
        self
      end

      def with_auth(auth)
        Preconditions.assert_class('auth', auth, HttpClient::Authorization)
        Preconditions.check_state(@auth.nil?, "auth previously set")

        if auth.scheme.name == AuthScheme::BASIC.name
          @auth = auth
        else
          raise "Auth Scheme[#{auth.scheme.name}] not supported"
        end
        self
      end

      def with_query(params)
        Preconditions.assert_class('params', params, Hash)
        Preconditions.check_state(@params.nil?, "Already have query parameters")
        @params = params
        self
      end

      # Wrapper to set Content-Type header to application/json and set
      # the provided json document as the body
      def with_json(json)
        @headers['Content-Type'] ||= 'application/json'
        with_body(json)
      end

      def with_body(body)
        Preconditions.check_not_blank('body', body)
        @body = body
        self
      end

      # Creates a new Net:HTTP client. The client returned should be
      # fully configured to make a request.
      def new_http_client
        client = Net::HTTP.new(@uri.host, @uri.port)
        if @uri.scheme == "https"
          configure_ssl(client)
        end
        client
      end

      # If HTTP is required, this method accepts an HTTP Client and configures SSL
      def configure_ssl(client)
        Preconditions.assert_class('client', client, Net::HTTP)
        client.use_ssl = true
        client.verify_mode = OpenSSL::SSL::VERIFY_PEER
        client.cert_store = OpenSSL::X509::Store.new
        client.cert_store.set_default_paths
      end

      def get(&block)
        do_request(Net::HTTP::Get, &block)
      end

      def delete(&block)
        do_request(Net::HTTP::Delete, &block)
      end

      def options(&block)
        do_request(Net::HTTP::Options, &block)
      end

      def post(&block)
        do_request(Net::HTTP::Post, &block)
      end

      def put(&block)
        do_request(Net::HTTP::Put, &block)
      end

      def do_request(klass)
        Preconditions.assert_class('klass', klass, Class)

        uri = @uri.to_s
        if q = to_query(@params)
          uri += "?%s" % q
        end

        request = klass.send(:new, uri)

        curl = ['curl']
        if klass != Net::HTTP::Get
          curl << "-X%s" % klass.name.split("::").last.upcase
        end

        if @body
          # DEBUG path = "/tmp/rest_client.tmp"
          # DEBUG File.open(path, "w") { |os| os << @body.to_s }
          # DEBUG curl << "-d@%s" % path
          request.body = @body
        end

        if @auth
          curl << "-u \"%s:%s\"" % [@auth.username, @auth.password]
          Preconditions.check_state(!@header_keys_lower_case.include?("authorization"),
                                    "Cannot specify both an Authorization header and an explicit username")
          user_pass = "%s:%s" % [@auth.username, @auth.password]
          encoded = Base64.encode64(user_pass).to_s.split("\n").map(&:strip).join
          request.add_field("Authorization", "Basic %s" % encoded)
        end

        @headers.each do |key, value|
          curl <<  "-H \"%s: %s\"" % [key, value]
          request.add_field(key, value)
        end

        curl << "'%s'" % uri
        # DEBUG puts curl.join(" ")

        raw_response = http_request(request)
        response = raw_response.to_s == "" ? nil : JSON.parse(raw_response)

        if block_given?
          yield response
        else
          response
        end
      end

      private
      def to_query(params={})
        parts = (params || {}).map do |k,v|
          if v.respond_to?(:each)
            v.map { |el| "%s=%s" % [k, CGI.escape(el.to_s)] }
          else
            "%s=%s" % [k, CGI.escape(v.to_s)]
          end
        end
        parts.empty? ? nil : parts.join("&")
      end

      def http_request(request)
        response = new_http_client.request(request)

        case response
        when Net::HTTPSuccess
          response.body
        else
          body = response.body rescue nil
          raise HttpClient::ServerError.new(response.code.to_i, response.message, :body => body)
        end
      end
    end

    class ServerError < StandardError

      attr_reader :code, :details, :body

      def initialize(code, details, incoming={})
        opts = HttpClient::Helper.symbolize_keys(incoming)
        @code = HttpClient::Preconditions.assert_class('code', code, Integer)
        @details = HttpClient::Preconditions.assert_class('details', details, String)
        @body = HttpClient::Preconditions.assert_class_or_nil('body', opts.delete(:body), String)
        HttpClient::Preconditions.assert_empty_opts(opts)
        super(self.message)
      end

      def message
        m = "%s %s" % [@code, @details]
        if @body
          m << ": %s" % @body
        end
        m
      end

      def body_json
        JSON.parse(@body)
      end

    end

    module Preconditions

      def Preconditions.check_argument(expression, error_message=nil)
        if !expression
          raise error_message || "check_argument failed"
        end
        nil
      end

      def Preconditions.check_state(expression, error_message=nil)
        if !expression
          raise error_message || "check_state failed"
        end
        nil
      end

      def Preconditions.check_not_nil(field_name, reference, error_message=nil)
        if reference.nil?
          raise error_message || "argument for %s cannot be nil" % field_name
        end
        reference
      end

      def Preconditions.check_not_blank(field_name, reference, error_message=nil)
        if reference.to_s.strip == ""
          raise error_message || "argument for %s cannot be blank" % field_name
        end
        reference
      end

      # Throws an error if opts is not empty. Useful when parsing
      # arguments to a function
      def Preconditions.assert_empty_opts(opts)
        if !opts.empty?
          raise "Invalid opts: #{opts.keys.inspect}\n#{opts.inspect}"
        end
      end

      # Asserts that value is not nill and is_?(klass). Returns
      # value. Common use is
      #
      # amount = Preconditions.assert_class('amount', amount, BigDecimal)
      def Preconditions.assert_class(field_name, value, klass)
        Preconditions.check_not_nil('field_name', field_name)
        Preconditions.check_not_nil('klass', klass)
        Preconditions.check_not_nil('value', value, "Value for %s cannot be nil. Expected an instance of class %s" % [field_name, klass.name])
        Preconditions.check_state(value.is_a?(klass),
                                  "Value for #{field_name} is of type[#{value.class}] - class[#{klass}] is required. value[#{value.inspect.to_s}]")
        value
      end

      def Preconditions.assert_class_or_nil(field_name, value, klass)
        if !value.nil?
          Preconditions.assert_class(field_name, value, klass)
        end
      end

      def Preconditions.assert_boolean(field_name, value)
        Preconditions.check_not_nil('field_name', field_name)
        Preconditions.check_not_nil('value', value, "Value for %s cannot be nil. Expected an instance of TrueClass or FalseClass" % field_name)
        Preconditions.check_state(value.is_a?(TrueClass) || value.is_a?(FalseClass),
                                  "Value for #{field_name} is of type[#{value.class}] - class[TrueClass or FalseClass] is required. value[#{value.inspect.to_s}]")
        value
      end

      def Preconditions.assert_boolean_or_nil(field_name, value)
        if !value.nil?
          Preconditions.assert_boolean(field_name, value)
        end
      end

      def Preconditions.assert_collection_of_class(field_name, values, klass)
        Preconditions.assert_class(field_name, values, Array)
        values.each { |v| Preconditions.assert_class(field_name, v, klass) }
      end

      def Preconditions.assert_hash_of_class(field_name, hash, klass)
        Preconditions.assert_class(field_name, hash, Hash)
        values.each { |k, v| Preconditions.assert_class(field_name, v, klass) }
      end

    end

    class AuthScheme

      attr_reader :name

      def initialize(name)
        @name = HttpClient::Preconditions.check_not_blank('name', name)
      end

      BASIC = AuthScheme.new("basic") unless defined?(BASIC)

    end

    class Authorization

      attr_reader :scheme, :username, :password

      def initialize(scheme, username, opts={})
        @scheme = HttpClient::Preconditions.assert_class('schema', scheme, AuthScheme)
        @username = HttpClient::Preconditions.check_not_blank('username', username, "username is required")
        @password = HttpClient::Preconditions.assert_class_or_nil('password', opts.delete(:password), String)
        HttpClient::Preconditions.assert_empty_opts(opts)
      end

      def Authorization.basic(username, password=nil)
        Authorization.new(AuthScheme::BASIC, username, :password => password)
      end

    end

    module Helper

      def Helper.symbolize_keys(hash)
        Preconditions.assert_class('hash', hash, Hash)
        new_hash = {}
        hash.each do |k, v|
          new_hash[k.to_sym] = v
        end
        new_hash
      end

      def Helper.to_big_decimal(value)
        value ? BigDecimal.new(value.to_s) : nil
      end

      def Helper.to_object(value)
        value ? JSON.parse(value) : nil
      end

      def Helper.to_uuid(value)
        Preconditions.check_state(value.nil? || value.match(/^\w\w\w\w\w\w\w\w\-\w\w\w\w\-\w\w\w\w\-\w\w\w\w\-\w\w\w\w\w\w\w\w\w\w\w\w$/),
                                  "Invalid guid[%s]" % value)
        value
      end

      def Helper.to_date_iso8601(value)
        if value.is_a?(Date)
          value
        elsif value
          Date.parse(value.to_s)
        else
          nil
        end
      end

      def Helper.to_date_time_iso8601(value)
        if value.is_a?(DateTime)
          value
        elsif value
          DateTime.parse(value.to_s)
        else
          nil
        end
      end

      def Helper.date_iso8601_to_string(value)
        value.nil? ? nil : value.strftime('%Y-%m-%d')
      end

      def Helper.date_time_iso8601_to_string(value)
        value.nil? ? nil : value.strftime('%Y-%m-%dT%H:%M:%S%z')
      end

      TRUE_STRINGS = ['t', 'true', 'y', 'yes', 'on', '1', 'trueclass'] unless defined?(TRUE_STRINGS)
      FALSE_STRINGS = ['f', 'false', 'n', 'no', 'off', '0', 'falseclass'] unless defined?(FALSE_STRINGS)

      def Helper.to_boolean(field_name, value, opts={})
        string = value.to_s.strip.downcase
        if TRUE_STRINGS.include?(string)
          true
        elsif FALSE_STRINGS.include?(string)
          false
        elsif string != ""
          raise "Unsupported boolean value[#{string}]. For true, must be one of: #{TRUE_STRINGS.inspect}. For false, must be one of: #{FALSE_STRINGS.inspect}"
        else
          nil
        end
      end

    end

  end
end