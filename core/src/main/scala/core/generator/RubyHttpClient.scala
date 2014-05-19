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
        @uri = Preconditions.assert_class('uri', uri, URI)
        @params = nil
        @body = nil
        @auth = nil
        @headers = {}
        @headers_keys_lower_case = []
      end

      def with_header(name, value)
        Preconditions.check_not_blank('name', name, "Header name is required")
        Preconditions.check_not_blank('value', value, "Header value is required")
        Preconditions.check_state(!@headers.has_key?(name),
                                  "Duplicate header named[%s]" + name)
        @headers[name] = value
        @headers_keys_lower_case << name.downcase
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
        Preconditions.check_not_blank('json', json)
        @headers['Content-Type'] ||= 'application/json'
        @body = json
        self
      end

      def with_body(body)
        Preconditions.check_not_blank('body', body)
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
        http = Net::HTTP.new(@uri.host, @uri.port)
        if @uri.scheme == "https"
          http.use_ssl = true
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        end
        response = http.request(request)

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

    end

    class AuthScheme

      attr_reader :name

      def initialize(name)
        @name = HttpClient::Preconditions.check_not_blank('name', name)
      end

      BASIC = AuthScheme.new("basic")

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

    module Types

      class MoneyIso4217Type

        attr_reader :currency, :amount

        def initialize(currency, amount)
          @currency = HttpClient::Preconditions.assert_class('currency', currency, String)
          HttpClient::Preconditions.check_state(@currency.length == 3, "Currency[%s] must be exactly 3 characters long" % @currency)
          HttpClient::Preconditions.check_state(@currency.upcase == @currency, "Currency[%s] must be in upper case" % @currency)
          @amount = HttpClient::Preconditions.assert_class('amount', amount, BigDecimal)
        end

        def MoneyIso4217Type.from_string(value, opts={})
          required = opts.has_key?(:required) ? opts.delete(:required) : false
          HttpClient::Preconditions.assert_empty_opts(opts)

          if required
            HttpClient::Preconditions.assert_class('value', value, String)
          else
            HttpClient::Preconditions.assert_class_or_nil('value', value, String)
          end

          if value
            currency, amount = value.split(" ", 2)
            MoneyIso4217Type.new(currency, amount)
          else
            nil
          end
        end

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

      def Helper.to_model_instance(field_name, klass, value, opts={})
        HttpClient::Preconditions.assert_class(field_name, String)
        HttpClient::Preconditions.assert_class(klass, Class)
        required = opts.has_key?(:required) ? opts.delete(:required) : false
        HttpClient::Preconditions.assert_empty_opts(opts)

        if required
          Preconditions.assert_class(field_name, value, klass)
        else
          Preconditions.assert_class_or_nil(field_name, value, klass)
        end

        value ? klass.send(:new, value) : nil
      end

      def Helper.to_big_decimal(value, opts={})
        required = opts.has_key?(:required) ? opts.delete(:required) : false
        HttpClient::Preconditions.assert_empty_opts(opts)

        if required
          Preconditions.assert_class('value', value, String)
        else
          Preconditions.assert_class_or_nil('value', value, String)
        end

        value ? BigDecimal.new(value) : nil
      end

      def Helper.to_uuid(value, opts={})
        required = opts.has_key?(:required) ? opts.delete(:required) : false
        HttpClient::Preconditions.assert_empty_opts(opts)

        if required
          Preconditions.assert_class('value', value, String)
        else
          Preconditions.assert_class_or_nil('value', value, String)
        end

        if value
          Preconditions.check_state(value.match(/^\w\w\w\w\w\w\w\w\-\w\w\w\w\-\w\w\w\w\-\w\w\w\w\-\w\w\w\w\w\w\w\w\w\w\w\w$/),
                                    "Invalid guid[%s]" % value)
        end

        value
      end

      def Helper.to_date_time_is8601(value, opts={})
        required = opts.has_key?(:required) ? opts.delete(:required) : false
        HttpClient::Preconditions.assert_empty_opts(opts)

        if required
          Preconditions.assert_class('value', value, String)
        else
          Preconditions.assert_class_or_nil('value', value, String)
        end

        value ? DateTime.parse(value) : nil
      end

    end

  end
"""

}
