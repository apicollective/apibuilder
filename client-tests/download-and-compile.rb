#!/usr/bin/env ruby

load 'ruby_client.rb'

if arg_force = (ARGV[0] == "--force")
  ARGV.shift
end

service_uri = ARGV.shift
token = ARGV.shift
user_guid = ARGV.shift
if service_uri.to_s.strip == "" || token.to_s.strip == "" || user_guid.to_s == ""
  raise "service uri, token and user_guid required"
end

orgs = [] # ['gilt']
services = []  # ['apidoc']
services_to_skip_by_org = {
  "gilt" => ["transactional-email-delivery-service"] # Currently > 22 fields
}

if !arg_force && (!orgs.empty? || !services.empty?)
  puts "Confirm you would like to limit tests to:"
  puts "  Organization: " + (orgs.empty? ? "All" : orgs.join(", "))
  puts "      Services: " + (services.empty? ? "All" : services.join(", "))
  print "Continue? (y/n): "
  response = gets
  if !response.strip.downcase.match(/^y/)
    exit(0)
  end
end

class RubyTester

  CLIENT_DIR = "clients"

  def file_extension
    "rb"
  end

  def test
    system("ruby #{CLIENT_DIR}/*.downloaded.rb")
  end

  def clean!(platform)
    cmd = "rm -f #{platform}/#{CLIENT_DIR}/*.downloaded.rb"
    puts cmd
    system(cmd)
  end

  def write(platform, org_key, service_key, target_name, code)
    filename = File.join(platform, CLIENT_DIR, [org_key, service_key, "#{target_name}.downloaded.rb"].join("."))
    File.open(filename, "w") do |out|
      out << code
    end
    filename
  end

end

class ScalaTester

  def initialize(client_dir)
    @client_dir = client_dir
  end
  
  def test
    system("sbt compile")
  end

  def clean!(platform)
    cmd = "rm -f #{platform}/#{@client_dir}/*.downloaded.scala"
    puts cmd
    system(cmd)
  end

  def ensure_dir!(dir)
    if !File.directory?(dir)
      cmd = "mkdir -p #{dir}"
      puts cmd
      system(cmd)
    end
  end

  def write(platform, org_key, service_key, target_name, code)
    filename = File.join(platform, @client_dir, [org_key, service_key, "#{target_name}.downloaded.scala"].join("."))
    ensure_dir!(File.dirname(filename))

    File.open(filename, "w") do |out|
      out << code
    end
    filename
  end

end

class Target

  attr_reader :platform, :tester, :names

  def initialize(platform, tester, names)
    @platform = platform
    @tester = tester
    @names = names
  end

end

targets = [Target.new('ning_1_8', ScalaTester.new("src/main/scala"), ['ning_1_8_client', 'play_2_x_json', 'scala_models']),
           Target.new('ruby', RubyTester.new, ['ruby_client']),
           Target.new('play_2_2', ScalaTester.new("app/models"), ['play_2_2_client', 'play_2_x_json', 'scala_models']),
           Target.new('play_2_3', ScalaTester.new("app/models"), ['play_2_3_client', 'play_2_x_json', 'scala_models'])]

def get_code(client, org, service, target)
  client.code.get_by_org_key_and_service_key_and_version_and_generator_key(org.key, service.key, "latest", target)
end

class MyClient < Apidoc::Client

  def initialize(url, user_guid, opts={})
    super(url, opts)
    @user_guid = user_guid
  end

  def request(*args)
    super.with_header("X-User-Guid", @user_guid)
  end

end


client = MyClient.new(service_uri, user_guid, :authorization => Apidoc::HttpClient::Authorization.basic(token))

CACHE = {}

def get_in_batches(cache_key, fetcher)
  offset = 0
  limit = 100
  records = nil
  while records.nil? || records.size >= limit
    cache_key = "%s?limit=%s&offset=%s" % [cache_key, limit, offset]
    CACHE[cache_key] ||= fetcher.call(limit, offset)
    records = CACHE[cache_key]
    records.each do |rec|
      yield rec
    end
    offset += limit
  end
end

targets.each do |target|
  target.names.each do |target_name|
    puts "Platform: %s, target: %s" % [target.platform, target_name]
    puts "--------------------------------------------------"

    target.tester.clean!(target.platform)
    get_in_batches("organizations", lambda { |limit, offset| client.organizations.send(:get, :limit => limit, :offset => offset) }) do |org|
      next if !orgs.empty? && !orgs.include?(org.key)
      services_to_skip = services_to_skip_by_org[org.key] || []

      get_in_batches("services:#{org.key}", lambda { |limit, offset| client.services.get_by_org_key(org.key, :limit => limit, :offset => offset) }) do |service|
        next if !services.empty? && !services.include?(service.key)
        next if services_to_skip.include?(service.key)

        puts "  %s/%s" % [org.key, service.key]
        if code = get_code(client, org, service, target_name)
          filename = target.tester.write(target.platform, org.key, service.key, target_name, code.source)
        end
      end
    end

    puts ""
    puts "  Testing in ./#{target.platform}"
    Dir.chdir(target.platform) do
      if target.tester.test
        puts "  - %s: Client tests passed\n\n" % target_name
      else
        puts "  - %s: Client tests failed\n\n" % target_name
        exit 1
      end
    end
  end
end
