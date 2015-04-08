#!/usr/bin/env ruby

CLI_PATH = "/web/apidoc-cli/bin/apidoc"

if !File.exists?(CLI_PATH)
  puts "ERROR: Could not find CLI at #{CLI_PATH}"
  puts ""
  exit(1)
end

load '/web/apidoc-cli/lib/apidoc-cli/args.rb'

args = ApidocCli::Args.parse(ARGV)

PROFILE = args[:profile]

orgs = [] # ['gilt']
applications = []  # ['apidoc', 'apidoc-spec', 'apidoc-generator']

if !args.has_key?(:force) && (!orgs.empty? || !applications.empty?)
  puts "Confirm you would like to limit tests to:"
  puts "  Organization: " + (orgs.empty? ? "All" : orgs.join(", "))
  puts "  Applications: " + (applications.empty? ? "All" : applications.join(", "))
  print "Continue? (y/n): "
  response = $stdin.gets
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

  def write(platform, org_key, application_key, target_name, code)
    filename = File.join(platform, CLIENT_DIR, [org_key, application_key, "#{target_name}.downloaded.rb"].join("."))
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

  def write(platform, org_key, application_key, target_name, code)
    filename = File.join(platform, @client_dir, [org_key, application_key, "#{target_name}.downloaded.scala"].join("."))
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

targets = [
           Target.new('ning_1_9_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_9_client', 'scala_models']),
           Target.new('ning_1_8_scala_2_10', ScalaTester.new("src/main/scala"), ['ning_1_8_client']),
           Target.new('ning_1_8_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_8_client']),
           Target.new('ruby', RubyTester.new, ['ruby_client']),
           Target.new('play_2_3', ScalaTester.new("app/models"), ['play_2_3_client', 'play_2_x_json', 'scala_models']),
           Target.new('play_2_2', ScalaTester.new("app/models"), ['play_2_2_client'])
          ]

def cli(command, opts={})
  limit = opts.delete(:limit)
  offset = opts.delete(:offset)
  if !opts.empty?
    raise "Invalid keys: #{opts.keys.inspect}"
  end

  builder = {
    "LIMIT" => limit.to_s.strip,
    "OFFSET" => offset.to_s.strip,
    "PROFILE" => (PROFILE.to_s.strip == "") ? "" : PROFILE.to_s.strip
  }.select { |k, v| v != "" }.map { |k,v| "export #{k}=#{v}" }

  builder << "#{CLI_PATH} #{command}"
  cmd = builder.join(" && ")
  # puts cmd
  `#{cmd}`.strip
end

def get_code(org, application, generator)
  version = "latest"

  begin
    code = cli("code #{org} #{application} #{version} #{generator}")
  rescue Com::Gilt::Apidoc::Api::V0::HttpClient::ServerError => e
    if e.code == 404
      puts " - warning: application[#{application}] either has no versions or was otherwise not found"
    else
      raise "Failed to fetch code for org[#{org}] application[#{application}] version[#{version}] generator[#{generator}]"
    end
  end

  code
end

CACHE = {}

def get_in_batches(cli_command)
  offset = 0
  limit = 100
  records = nil
  while records.nil? || records.size >= limit
    cache_key = "%s?limit=%s&offset=%s" % [cli_command, limit, offset]
    CACHE[cache_key] ||= cli(cli_command, :limit => limit, :offset => offset).split("\n").map(&:strip)

    records = CACHE[cache_key]
    records.each do |rec|
      yield rec
    end
    offset += limit
  end
end

targets.each do |target|
  target.names.each do |generator|
    puts "Platform: %s, target: %s" % [target.platform, generator]
    puts "--------------------------------------------------"

    target.tester.clean!(target.platform)

    get_in_batches("list organizations") do |org|
      next if !orgs.empty? && !orgs.include?(org)

      get_in_batches("list applications #{org}") do |app|
        next if !applications.empty? && !applications.include?(app)

        puts "  %s/%s" % [org, app]
        if code = get_code(org, app, generator)
          filename = target.tester.write(target.platform, org, app, generator, code)
        end
      end
    end

    puts ""
    puts "  Testing in ./#{target.platform}"
    Dir.chdir(target.platform) do
      if target.tester.test
        puts "  - %s: Client tests passed\n\n" % generator
      else
        puts "  - %s: Client tests failed\n\n" % generator
        exit 1
      end
    end
  end
end
