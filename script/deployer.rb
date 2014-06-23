module Deployer

  class Commands

    MAX_INDEX = 10000

    attr_reader :project_name, :tag, :dir

    def initialize(project_name, tag)
      @project_name = Preconditions.assert_class(project_name, String)
      @tag = Preconditions.assert_class(tag, String)
      @dir = Commands.create_unique_dir("/tmp/deployer-%s-%s" % [@project_name, @tag])
      @index = 1001
    end

    # cmd: A string or an array of strings for the commands to run
    def add(cmd, opts={})
      name = opts.delete(:name)
      Preconditions.check_state(opts.empty?, "Invalid opts: %s" % opts.inspect)
      Preconditions.check_state(@index < MAX_INDEX, "Too many commands (would fail lexicographic sorting if we added this command)")

      filename = @index.to_s
      if name
        filename << "-"
        filename << name.downcase.gsub(/[^a-zA-Z0-9\-\_]/, '-').gsub(/\_/, '-').gsub(/\-\-+/, '-')
      end

      path = File.join(@dir, filename)
      @index += 1

      File.open(path, "w", 0777) do |out|
        out << "#\!/bin/sh\n"
        out << "set -x #echo on\n"
        out << "\n"

        if cmd.respond_to?(:join)
          out << cmd.join("\n")
        else
          out << cmd
        end
        out << "\n"
      end
    end

    def run!
      Deployer::Executor.new(@dir).run
    end

    private
    def Commands.create_unique_dir(base)
      @project_name = Preconditions.assert_class(base, String)

      tmp_dir = base.dup
      count = 1
      while File.exists?(tmp_dir)
        tmp_dir = "%s-%s" % [base, count]
        count += 1
        Preconditions.check_state(count <= 1000, "Failed to create a unique directory name with a base of %s" % base)
      end

      Util.run("mkdir #{tmp_dir}")
      tmp_dir
    end

  end

  class Executor

    def initialize(dir)
      @dir = Preconditions.assert_class(dir, String)
    end

    def run
      files = Dir.glob("#{@dir}/1*").sort
      files.each_with_index do |path, i|
        msg = "#{i + 1}/#{files.length}: #{path}"
        puts msg
        puts "-" * (msg.length)
        puts Time.now.to_s
        result = system(path)
        Preconditions.check_state(result, "Failed: %s" % $?)
        puts ""
      end
    end

  end

  module Util

    def Util.next_tag
      if !system("which sem-info")
        raise "Install SEM: https://github.com/gilt/schema-evolution-manager/"
      end

      tag = `sem-info tag next`.strip
      Preconditions.check_not_blank(tag, "Missing tag")
    end

    def Util.run(cmd)
      puts cmd
      system(cmd)
    end

    def Util.env(name)
      value = ENV[name].to_s
      Preconditions.check_not_blank(value, "Missing environment variable[%s]" % name)
    end

  end

  module Preconditions

    def Preconditions.assert_class(value, klass)
      Preconditions.check_not_null(klass, "Klass cannot be nil")
      Preconditions.check_not_null(value, "Value cannot be nil - expected an instance of type %s" % klass)
      Preconditions.check_state(value.is_a?(klass),
                                "Value is of type[#{value.class}] - class[#{klass}] is required")
      value
    end

    def Preconditions.check_argument(expression, error_message=nil)
      if !expression
        raise error_message || "check_argument failed"
      end
      nil
    end

    def Preconditions.check_state(expression, error_message=nil)
      if !expression
        raise error_message || "check_state failed"
      end
      nil
    end

    def Preconditions.check_not_null(reference, error_message=nil)
      if reference.nil?
        raise error_message || "argument cannot be nil"
      end
      reference
    end

    def Preconditions.check_not_blank(reference, error_message=nil)
      if reference.to_s.strip == ""
        raise error_message || "argument cannot be blank"
      end
      reference
    end


  end

end
