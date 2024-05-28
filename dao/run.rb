#!/usr/bin/env ruby

profile = nil
remainder = []
i = 0
while i < ARGV.length
  v = ARGV[i]
  if v.match(/^\-\-/)
    if v == "--profile"
      i += 1
      profile = ARGV[i]
    else
      raise "Invalid flag: #{v}"
    end
  else
    remainder << v
  end
  i += 1
end

files = if remainder.empty?
  Dir.glob("spec/*json")
else
  remainder
end

def run(cmd)
  puts cmd
  if !system(cmd)
    exit 1
  end
end

def setup_dir(name)
  if File.directory?(name)
    run "rm -rf #{name}/*"
  else
    run "mkdir #{name}"
  end
end

setup_dir("scala")
setup_dir("psql")

files.each_with_index do |f, i|
  puts "\n"
  if i > 0
    puts "-" * 80
    puts "\n"
  end
  p = profile ? " --profile #{profile}" : ""
  run "~/code/flowcommerce/dao_generator/run.rb#{p} --organization apicollective #{f}"
end
