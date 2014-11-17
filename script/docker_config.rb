# Reads a configuration file similar in structure to the aws config file. Example:
#
# [default]
# 
# [profile gilt-architecture]
# account = giltarchitecture
# username = mbryzek
# email = mbryzek@alum.mit.edu
# password = xxx
# 
module Deployer

  class DockerConfig

    DEFAULT_PROFILE = "default" unless defined?(DEFAULT_PROFILE)
    DEFAULT_PATH = "~/.docker/config" unless defined?(DEFAULT_PATH)

    def initialize(opts={})
      @path = Preconditions.assert_class(opts.delete(:path) || File.expand_path(DEFAULT_PATH), String)
      Preconditions.check_state(File.exists?(@path), "Docker config file[#{@path}] not found")

      @profiles = {}
      profile = nil

      IO.readlines(@path).each_with_index do |line, i|
        stripped = line.strip
        if stripped == ""
          next
        end

        if md = stripped.match(/^\[(.+)\]$/)
          key, value = md[1].strip.split(/\s+/, 2).map(&:strip)
          if key == "default"
            Preconditions.check_state(value.to_s == "", "%s:%s value attribute[%s] not supported for key default" % [@path, i+1, value])
            profile = "default"

          elsif key == "profile"
            Preconditions.check_state(value.to_s != "", "%s:%s profile attribute missing name" % [@path, i+1])
            profile = value
          else
            raise "%s:%s unknown configuration key[%s]" % [@path, i+1, key]
          end

          Preconditions.check_state(!@profiles.has_key?(profile), "%s:%s duplicate profile[%s]" % [@path, i+1, profile])
          @profiles[profile] = Profile.new(profile)

        elsif profile
          name, value = stripped.split(/\s*=\s*/, 2).map(&:strip)
          @profiles[profile].add(name, value)
        end
      end
    end

    # Returns the Profile instance w/ the specified name
    def get(profile)
      Preconditions.assert_class(profile, String)
      config = Preconditions.check_not_null(@profiles[profile], "Profile[#{profile}] not found")
      config.assert_complete
      config
    end

  end

  class Profile

    attr_reader :name

    REQUIRED_KEYS = [:account, :email, :password, :username]

    def initialize(name)
      @name = Preconditions.assert_class(name, String)
      @data = {}
    end

    def assert_complete
      missing = REQUIRED_KEYS.select { |k| @data[k].to_s.strip == ""}
      if !missing.empty?
        raise "Profile[#{@name}] is missing keys: #{missing.join(", ")}"
      end
    end

    def add(key, value)
      Preconditions.assert_class(key, String)
      Preconditions.assert_class(value, String)
      Preconditions.check_state(!@data.has_key?(key), "Profile[#{@name}] duplicate key[#{key}]")

      @data[key.to_sym] = value
    end

    def account
      @data[:account]
    end

    def email
      @data[:email]
    end

    def password
      @data[:password]
    end

    def username
      @data[:username]
    end

  end

end
