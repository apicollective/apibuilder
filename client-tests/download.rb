#!/usr/bin/env ruby

load 'ruby_client.rb'

token = ARGV.shift
if token.to_s.strip == ""
  raise "Token required"
end

client = ApiDoc::Client.new("http://api.apidoc.me", :authorization => ApiDoc::HttpClient::Authorization.basic(token))
client.organizations.get.each do |org|
  puts org.name
  puts "--------------------------------------------------"
  client.services.get_by_org_key(org.key).each do |service|
    puts "-- " + service.key
    if version = client.versions.get_by_org_key_and_service_key_and_version(org.key, service.key, "latest")
      puts version.json
    end
  end
end

