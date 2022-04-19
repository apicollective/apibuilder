#!/usr/bin/env ruby

files = if ARGV.empty?
  Dir.glob("spec/*json")
else
  ARGV
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
  run "~/code/flowcommerce/hack_dao_generator/run.rb --organization apicollective #{f}"
end
