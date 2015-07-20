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
#           Target.new('play_2_4', ScalaTester.new("app/models"), ['play_2_4_client', 'play_2_x_json', 'scala_models']),
  Target.new('play_2_3', ScalaTester.new("app/models"), ['play_2_3_client']),
  Target.new('ning_1_9_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_9_client', 'scala_models']),
           Target.new('ruby', RubyTester.new, ['ruby_client']),
           Target.new('play_2_3', ScalaTester.new("app/models"), ['play_2_3_client']),
           Target.new('play_2_2', ScalaTester.new("app/models"), ['play_2_2_client']),
           Target.new('ning_1_8_scala_2_10', ScalaTester.new("src/main/scala"), ['ning_1_8_client']),
           Target.new('ning_1_8_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_8_client'])
          ]

def cli(command, opts={})
  limit = opts.delete(:limit)
  offset = opts.delete(:offset)
  has_version = opts.delete(:has_version)
  if !opts.empty?
    raise "Invalid keys: #{opts.keys.inspect}"
  end

  builder = {
    "LIMIT" => limit.to_s.strip,
    "OFFSET" => offset.to_s.strip,
    "HAS_VERSION" => has_version.to_s,
    "PROFILE" => (PROFILE.to_s.strip == "") ? "" : PROFILE.to_s.strip
  }.select { |k, v| v != "" }.map { |k,v| "export #{k}=#{v}" }

  builder << "#{CLI_PATH} #{command}"
  cmd = builder.join(" && ")
  puts cmd
  `#{cmd}`.strip
end

def each_target(org, application, generator)
  version = "latest"

  get_files(org, application, generator, version).each do |file|
    code = cli("code #{org} #{application} #{version} #{generator} #{file}")
    yield file, code
  end
end

def get_files(org, application, generator, version)
  output = cli("code #{org} #{application} #{version} #{generator}")
  output.split("\n").select { |l| l.strip.match(/^\-/) }.map do |l|
    l.strip.sub(/^\-\s*/, '')
  end
end

CACHE = {}

def get_in_batches(cli_command, opts={})
  has_version = opts.delete(:has_version)
  if !opts.empty?
    raise "Invalid keys: #{opts.keys.inspect}"
  end

  offset = 0
  limit = 100
  records = nil
  while records.nil? || records.size >= limit
    cache_key = "%s?limit=%s&offset=%s&has_version=%s" % [cli_command, limit, offset, has_version]
    CACHE[cache_key] ||= cli(cli_command, :limit => limit, :offset => offset, :has_version => has_version).split("\n").map(&:strip)

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

      get_in_batches("list applications #{org}", :has_version => true) do |app|
        next if !applications.empty? && !applications.include?(app)

        puts "  %s/%s" % [org, app]
        each_target(org, app, generator) do |filename, code|
          # filename = target.tester.write(target.platform, org, app, generator, code)
          filename = target.tester.write(target.platform, org, app, filename, code)
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
