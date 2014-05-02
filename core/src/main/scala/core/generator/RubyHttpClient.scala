package core.generator

object RubyHttpClient {

  val require = """
require 'cgi'
require 'net/http'
require 'uri'
require 'base64'

require 'rubygems'
require 'json'
require 'bigdecimal'
"""

  val contents = """
  module HttpClient

    class Request

      def initialize(uri)
        @uri = Preconditions.assert_class(uri, URI)
        @params = nil
        @body = nil
        @auth = nil
        @headers = {}
        @headers_keys_lower_case = []
      end

      def with_header(name, value)
        Preconditions.check_not_blank(name, "Header name is required")
        Preconditions.check_not_blank(value, "Header value is required")
        Preconditions.check_state(!@headers.has_key?(name),
                                  "Duplicate header named[%s]" + name)
        @headers[name] = value
        @headers_keys_lower_case << name.downcase
        self
      end

      def with_auth(auth)
        Preconditions.assert_class(auth, HttpClient::Authorization)
        Preconditions.check_state(@auth.nil?, "auth previously set")

        if auth.scheme.name == AuthScheme::BASIC.name
          @auth = auth
        else
          raise "Auth Scheme[#{auth.scheme.name}] not supported"
        end
        self
      end

      def with_query(params)
        Preconditions.assert_class(params, Hash)
        Preconditions.check_state(@params.nil?, "Already have query parameters")
        @params = params
        self
      end

      # Wrapper to set Content-Type header to application/json and set
      # the provided json document as the body
      def with_json(json)
        Preconditions.check_not_blank(json)
        @headers['Content-Type'] ||= 'application/json'
        @body = json
        self
      end

      def with_body(body)
        Preconditions.check_not_blank(body)
        @body = body
        self
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
        Preconditions.assert_class(klass, Class)

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
          path = "/tmp/rest_client.tmp"
          File.open(path, "w") { |os| os << @body.to_s }
          curl << "-d@%s" % path
          request.body = @body
        end

        if @auth
          curl << "-u \"%s:%s\"" % [@auth.username, @auth.password]
          Preconditions.check_state(!@headers_keys_lower_case.include?("authorization"),
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
        #DEBUGGING: puts curl.join(" ")

        response = JSON.parse(http_request(request))

        if block_given?
          yield response
        else
          response
        end
      end

      private
      def to_query(params={})
        parts = (params || {}).map do |k,v|
          "%s=%s" % [k, CGI.escape(v.to_s)]
        end
        parts.empty? ? nil : parts.join("&")
      end

      def http_request(request)
        response = Net::HTTP.start(@uri.host, @uri.port) { |http|
          http.request(request)
        }
        case response
        when Net::HTTPSuccess
          response.body
        else
          response.error!
        end
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

      def Preconditions.check_not_nil(reference, error_message=nil)
        if reference.nil?
          raise error_message || "argument cannot be nil"
        end
        reference
      end

      def Preconditions.check_not_blank(reference, error_message=nil)
        if reference.to_s.strip == ""
          raise error_message || "argument cannot be blank"
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
      # amount = Preconditions.assert_class(amount, BigDecimal)
      def Preconditions.assert_class(value, klass)
        Preconditions.check_not_nil(klass, "Klass cannot be nil")
        Preconditions.check_not_nil(value, "Value cannot be nil. Expected an instance of class %s" % klass.name)
        Preconditions.check_state(value.is_a?(klass),
                                  "Value is of type[#{value.class}] - class[#{klass}] is required. value[#{value.inspect.to_s}]")
        value
      end

      def Preconditions.assert_class_or_nil(value, klass)
        if !value.nil?
          Preconditions.assert_class(value, klass)
        end
      end

    end

    class AuthScheme

      attr_reader :name

      def initialize(name)
        @name = HttpClient::Preconditions.check_not_blank(name)
      end

      BASIC = AuthScheme.new("basic")

    end

    class Authorization

      attr_reader :scheme, :username, :password

      def initialize(scheme, username, opts={})
        @scheme = HttpClient::Preconditions.assert_class(scheme, AuthScheme)
        @username = HttpClient::Preconditions.check_not_blank(username, "username is required")
        @password = HttpClient::Preconditions.assert_class_or_nil(opts.delete(:password), String)
        HttpClient::Preconditions.assert_empty_opts(opts)
      end

      def Authorization.basic(username, password=nil)
        Authorization.new(AuthScheme::BASIC, username, :password => password)
      end

    end

    module Helper

      def Helper.symbolize_keys(hash)
        Preconditions.assert_class(hash, Hash)
        new_hash = {}
        hash.each do |k, v|
          new_hash[k.to_sym] = v
        end
        new_hash
      end

    end

  end
"""

}
