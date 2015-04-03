#!/usr/bin/env ruby

# Example:
# ./copy.rb --from_profile default  --to_profile localhost --from_org gilt

CLI_PATH = "/web/apidoc-cli/bin/apidoc"

if !File.exists?(CLI_PATH)
  puts "ERROR: Could not find CLI at #{CLI_PATH}"
  puts ""
  exit(1)
end

load '/web/apidoc-cli/lib/apidoc-cli.rb'

args = ApidocCli::Args.parse(ARGV)

from_profile = args[:from_profile]
to_profile = args[:to_profile]
from_org = args[:from_org]

if from_profile.nil? || to_profile.nil?
  puts "Missing args"
  exit(1)
end

def get_in_batches(block)
  offset = 0
  limit = 100
  records = nil
  while records.nil? || records.size >= limit
    records = block.call(limit, offset)
    records.each do |rec|
      yield rec
    end
    offset += limit
  end
end

def upsert_org(client, org)
  if client.organizations.get(:key => org.key).empty?
    puts "Creating #{org.key}"
    client.organizations.post(Com::Gilt::Apidoc::Api::V0::Models::OrganizationForm.new(:name => org.name,
                                                                                       :key => org.key,
                                                                                       :namespace => org.namespace,
                                                                                       :visibility => org.visibility,
                                                                                       :domains => org.domains.map(&:name)))
  end
end

def upsert_app(client, org, app)
  if client.applications.get_by_org_key(org.key, :key => app.key).empty?
    puts "Creating #{org.key}:#{app.key}"
    client.applications.post_by_org_key(org.key,
                                        Com::Gilt::Apidoc::Api::V0::Models::ApplicationForm.new(:name => app.name,
                                                                                                :key => app.key,
                                                                                                :description => app.description,
                                                                                                :visibility => app.visibility))
  end
end

def copy_version(client, org, app, version)
  puts "Creating #{org.key}:#{app.key}:#{version.version}"

  if client.versions.put_by_org_key_and_application_key_and_version(org.key, app.key, version.version,
                                        Com::Gilt::Apidoc::Api::V0::Models::VersionForm.new(
                                          :original_form => Com::Gilt::Apidoc::Api::V0::Models::OriginalForm.new(:type => version.original.type, :data => version.original.data)))
  end
end

from_client = ApidocCli::Config.client_from_profile(:profile => from_profile)
to_client = ApidocCli::Config.client_from_profile(:profile => to_profile)

puts "from[#{from_profile}] to[#{to_profile}]"

version = "latest"
params = from_org ? { :key => from_org } : {}
get_in_batches( lambda { |limit, offset| from_client.organizations.get(params.merge(:limit => limit, :offset => offset)) } ) do |org|
  next if org.key.match(/^copy\-/i)
  to_org = org.copy(:name => "Copy #{org.name}", :key => "copy-#{org.key}")
  upsert_org(to_client, to_org)

  get_in_batches( lambda { |limit, offset| from_client.applications.get_by_org_key(org.key, :limit => limit, :offset => offset) } ) do |app|
    upsert_app(to_client, to_org, app)

    from_client.versions.get_by_org_key_and_application_key(org.key, app.key, :limit => 1).each do |version|
      if version.original.nil?
        puts "** Warning **: #{org.key}:#{app.key}:#{version.version} does not have an original. Skipping"
      else
        copy_version(to_client, to_org, app, version)
      end
    end
  end
end
