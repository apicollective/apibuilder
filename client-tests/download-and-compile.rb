#!/usr/bin/env ruby

load 'ruby_client.rb'
#APIDOC_URL = "http://api.apidoc.me"
APIDOC_URL = "http://localhost:9001"

CLIENT_DIR = "src/main/scala/clients"

token = ARGV.shift
if token.to_s.strip == ""
  raise "Token required"
end

targets = ['play_2_2_client', 'play_2_3_client', 'play_2_x_json', 'scala_models']
targets = ['play_2_3_client', 'play_2_x_json', 'scala_models']
orgs = ['gilt']
services = ['api-doc']

def write_target(org, service, target, code)
  filename = File.join(CLIENT_DIR, [org.key, service.key, "#{target.value}.downloaded.scala"].join("."))
  File.open(filename, "w") do |out|
    out << "package clienttests_#{target.value} {\n\n"
    out << code.source
    out << "\n\n}"
  end
  filename
end

cmd = "rm -f #{CLIENT_DIR}/*.downloaded.scala"
puts cmd
system(cmd)

client = ApiDoc::Client.new(APIDOC_URL, :authorization => ApiDoc::HttpClient::Authorization.basic(token))
client.organizations.get.each do |org|
  #next unless orgs.include?(org.key)
  puts org.name
  puts "--------------------------------------------------"
  client.services.get_by_org_key(org.key).each do |service|
    #next unless services.include?(service.key)
    puts "  " + service.key
    targets.each do |target_name|
      target = ApiDoc::Models::Target.send(target_name)
      if code = client.code.get_by_org_key_and_service_key_and_version_and_target(org.key, service.key, "latest", target)
        filename = write_target(org, service, target, code)
        puts "    #{target.value}: #{filename}"
      end
    end
  end
end

puts "Finished downloading clients. Compiling"
if system("sbt compile")
  puts "All clients compiled"
  exit 0
else
  puts "Clients failed to compile"
  exit 1
end
