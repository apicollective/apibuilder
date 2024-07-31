class Ask

  TRUE = ['y', 'yes']
  FALSE = ['n', 'no']
  
  def Ask.for_string(msg)
    value = ""
    while value.empty?
      puts msg
      value = STDIN.gets
      value.strip!
    end
    value
  end

  def Ask.for_boolean(msg)
    result = nil
    while result.nil?
      value = Ask.for_string(msg).downcase
      if TRUE.include?(value)
        result = true
      elsif FALSE.include?(value)
        result = false
      end
    end
    result
  end
  
end
  
