package models

object RubyHttpClient {

  val require = """
require 'cgi'
require 'net/http'
require 'net/https'
require 'uri'
require 'base64'

require 'rubygems'
require 'json'
require 'bigdecimal'
""".trim

  val contents = """
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
        @headers['Content-Type'] ||= 'application/json; charset=UTF-8'
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
""".trim

}
