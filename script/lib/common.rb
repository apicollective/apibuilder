Dir.glob("#{File.dirname(__FILE__)}/*.rb")
  .select { |f| File.basename(f) != "common.rb" }
  .each { |f| load f }

class Verbosity

  attr_reader :enabled

  def initialize
    @enabled = false
  end

  def set(value)
    @enabled = value ? true : false
  end
end

VERBOSITY = Verbosity.new unless defined?(VERBOSITY)

def err(msg)
  puts ""
  puts "ERROR"
  puts "  " + msg
  puts ""
  exit(1)
end

def run(cmd)
  puts "==> #{cmd}" if VERBOSITY.enabled
  if !system(cmd)
    err("Command failed: #{cmd}")
  end
end

def assert_installed(cmd, url)
  if !system("which %s > /dev/null" % cmd)
    err("Please install %s: %s" % [cmd, url])
  end
end

def assert_sem_installed
  assert_installed("sem-info", "https://github.com/mbryzek/schema-evolution-manager")
end


def args_from_stdin(opts={})
  arrays = opts.delete(:arrays) || []
  flags = opts.delete(:flags) || []
  flags << ":v"
  if !opts.empty?
    err("Unexpected options: #{opts.keys}")
  end

  args = {}

  i = 0
  while i < ARGV.length
    name = ARGV[i].to_s.strip.sub(/^\-\-/, '').to_sym
    i+=1
    if name
      if flags.include?(name)
        args[name] = true
      else
        value = ARGV[i]
        i+=1
        if arrays.include?(name)
          args[name] ||= []
          args[name] << value
        elsif args[name]
          err("Argument '#{name}' specified more than once")
        elsif value.start_with?("--")
          err("Argument '#{name}' missing value")
        else
          args[name] = value
        end
      end
    end
  end

  if args[:v]
    VERBOSITY.set(args[:v])
  end

  args
end
