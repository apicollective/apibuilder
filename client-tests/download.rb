#!/usr/bin/env ruby

load 'ruby_client.rb'
APIDOC_URL = "http://api.apidoc.me"
#APIDOC_URL = "http://localhost:9001"

token = ARGV.shift
if token.to_s.strip == ""
  raise "Token required"
end

targets = ['play_2_2_client', 'play_2_3_client', 'play_2_x_json', 'scala_models']

client = ApiDoc::Client.new(APIDOC_URL, :authorization => ApiDoc::HttpClient::Authorization.basic(token))
client.organizations.get.each do |org|
  puts org.name
  puts "--------------------------------------------------"
  client.services.get_by_org_key(org.key).each do |service|
    puts "-- " + service.key
    targets.each do |target_name|
      target = ApiDoc::Models::Target.send(target_name)
      if code = client.code.get_by_org_key_and_service_key_and_version_and_target(org.key, service.key, "latest", target)
        puts code
        exit 1
      end
    end
  end
end
