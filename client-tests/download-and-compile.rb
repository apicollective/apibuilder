#!/usr/bin/env ruby

CLI_PATH = "/web/apidoc-cli/bin/apidoc"

if arg_force = (ARGV[0] == "--force")
  ARGV.shift
end

APIDOC_API_URI = ARGV.shift.to_s.strip
if APIDOC_API_URI == ""
  raise "service uri is required"
end

orgs = [] # ['gilt']
applications = []  # ['apidoc']
applications_to_skip_by_org = {
  "gilt" => ["transactional-email-delivery-service"] # Currently > 22 fields
}

if !arg_force && (!orgs.empty? || !applications.empty?)
  puts "Confirm you would like to limit tests to:"
  puts "  Organization: " + (orgs.empty? ? "All" : orgs.join(", "))
  puts "  Applications: " + (applications.empty? ? "All" : applications.join(", "))
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
           Target.new('ning_1_9_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_9_client', 'play_2_x_json', 'scala_models']),
           Target.new('ning_1_8_scala_2_10', ScalaTester.new("src/main/scala"), ['ning_1_8_client']),
           Target.new('ning_1_8_scala_2_11', ScalaTester.new("src/main/scala"), ['ning_1_8_client']),
           Target.new('ruby', RubyTester.new, ['ruby_client']),
           Target.new('play_2_3', ScalaTester.new("app/models"), ['play_2_3_client', 'play_2_x_json', 'scala_models']),
           Target.new('play_2_2', ScalaTester.new("app/models"), ['play_2_2_client'])
          ]

def get_code(org, application, generator)
  version = "latest"

  cmd = "APIDOC_API_URI=#{APIDOC_API_URI} #{CLI_PATH} code #{org} #{application} #{version} #{generator}"
  puts cmd
  code = `#{cmd}`.to_s.strip
  if code == ""
    raise "Failed to fetch code for org[#{org}] application[#{application}] version[#{version}] generator[#{generator}]"
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
    if CACHE[cache_key].nil?
      cmd = "LIMIT=#{limit} OFFSET=#{offset} APIDOC_API_URI=#{APIDOC_API_URI} #{CLI_PATH} #{cli_command}"
      CACHE[cache_key] = `#{cmd}`.strip.split("\n").map(&:strip)
    end

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
      applications_to_skip = applications_to_skip_by_org[org] || []

      get_in_batches("list applications #{org}") do |app|
        next if !applications.empty? && !applications.include?(app)
        next if applications_to_skip.include?(app)

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
