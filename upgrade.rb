#!/usr/bin/env ruby

`find . -type f -name "*.scala" | grep -v target`.strip.split("\n").each do |file|
  tmp = []
  found = false
  IO.readlines(file).each do |line|
    if md = line.match(/Symbol\(\"(.+)\"\)/)
      found = true
      tmp << line.sub(/Symbol\(\".+\"\)/, "\"#{md[1]}\"")
    else
      tmp << line
    end
  end
  if found
    File.open(file, "w") {|f| f << tmp.join("") }
    puts "Rewriting file #{file}"
  end
end
