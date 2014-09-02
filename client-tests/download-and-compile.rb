#!/usr/bin/env ruby

load 'ruby_client.rb'

service_uri = ARGV.shift

token = ARGV.shift
if service_uri.to_s.strip == "" || token.to_s.strip == ""
  raise "service uri and token required"
end

orgs = [] # ['gilt']
services = []  # ['api-doc']

if !orgs.empty? || !services.empty?
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
      out << "module Clienttests_#{target_name}\n\n"
      out << code
      out << "\n\nend"
    end
    filename
  end

end

class ScalaTester

  CLIENT_DIR = "app/models"

  def test
    system("sbt compile")
  end

  def clean!(platform)
    cmd = "rm -f #{platform}/#{CLIENT_DIR}/*.downloaded.scala"
    puts cmd
    system(cmd)
  end

  def write(platform, org_key, service_key, target_name, code)
    filename = File.join(platform, CLIENT_DIR, [org_key, service_key, "#{target_name}.downloaded.scala"].join("."))
    File.open(filename, "w") do |out|
      out << "package clienttests_#{target_name} {\n\n"
      out << code
      out << "\n\n}"
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

targets = [Target.new('ruby', RubyTester.new, ['ruby_client']),
           Target.new('play_2_2', ScalaTester.new, ['play_2_2_client', 'play_2_x_json', 'scala_models']),
           Target.new('play_2_3', ScalaTester.new, ['play_2_3_client', 'play_2_x_json', 'scala_models'])]

def get_code(client, org, service, target)
  client.code.get_by_org_key_and_service_key_and_version_and_target(org.key, service.key, "latest", target)
end

client = ApiDoc::Client.new(service_uri, :authorization => ApiDoc::HttpClient::Authorization.basic(token))

def get_orgs_in_batches(client)
  offset = 0
  limit = 100
  records = nil
  while records.nil? || !records.empty?
    records = client.organizations.send(:get, :limit => limit, :offset => offset)
    records.each do |rec|
      yield rec
    end
    offset += limit
  end
end

def get_services_in_batches(client, org)
  offset = 0
  limit = 100
  records = nil
  while records.nil? || !records.empty?
    records = client.services.get_by_org_key(org.key, :limit => limit, :offset => offset)
    records.each do |rec|
      yield rec
    end
    offset += limit
  end
end

targets.each do |target|
  puts "Platform: #{target.platform}"
  puts "--------------------------------------------------"
  target.tester.clean!(target.platform)
  get_orgs_in_batches(client) do |org|
    next if !orgs.empty? && !orgs.include?(org.key)
    get_services_in_batches(client, org) do |service|
      next if !services.empty? && !services.include?(service.key)
      puts "  %s/%s" % [org.key, service.key]
      target.names.each do |target_name|
        t = ApiDoc::Models::Target.send(target_name)
        if code = get_code(client, org, service, t)
          filename = target.tester.write(target.platform, org.key, service.key, t.value, code.source)
          puts "    #{t.value}: #{filename}"
        end
      end
    end
  end

  puts ""
  puts "  Testing in ./#{target.platform}"
  Dir.chdir(target.platform) do
    if target.tester.test
      puts "  - Client tests passed\n\n"
    else
      puts "  - Client tests failed\n\n"
      exit 1
    end
  end
end
