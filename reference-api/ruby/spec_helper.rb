require 'client.rb'
require 'faker'
require 'uuid'

include ReferenceApi

def make_uuid
  @@UUID ||= UUID.new
  @@UUID.generate
end

def make_boolean
  [true, false].sample
end

def make_organization
  Models::Organization.new(
    :guid => make_uuid,
    :name => Faker::Company.name
  )
end

def make_user
  Models::User.new(
    :guid => make_uuid,
    :email => Faker::Internet.email,
    :active => make_boolean
  )
end

def make_member
  Models::Member.new(
    :guid => make_uuid,
    :organization => make_organization.to_h,
    :user => make_user.to_h,
    :role => Faker::Lorem.word
  )
end

class Object
  # generated client methods and model constructors
  # are expecting lot's of input to just be hashes...
  def to_h
    case self
    when String, Integer, TrueClass, FalseClass
      self
    else
      instance_variables.inject({}) { |h,v|
        h[v[1..-1].to_sym] = instance_variable_get(v).to_h
        h
      }
    end
  end
end
