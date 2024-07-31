class Tag
  def Tag.ask
    assert_sem_installed

    next_standard_tag = `sem-info tag next`.strip
    next_tag = replace_hundreds(next_standard_tag)
    puts ""
    if Ask.for_boolean("Create new tag #{next_tag}?")
      run("git tag -a -m #{next_tag} #{next_tag}")
      run("git push --tags origin")
    end

    `sem-info tag latest`.strip
  end

  def Tag.replace_hundreds(tag)
    parts = tag.split('.', 3)
    if parts.length == 3 && parts.all? { |p| p.to_i.to_s == p }
      if parts[2].to_i >= 100
        Tag.replace_hundreds("%s.%s.%s" % [parts[0], parts[1].to_i + 1, 0])
      elsif parts[1].to_i >= 100
        Tag.replace_hundreds("%s.%s.%s" % [parts[0] + 1, 0, 0])
      else
        tag
      end
    else
      tag
    end
  end
end
