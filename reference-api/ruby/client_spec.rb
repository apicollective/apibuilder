require 'spec_helper.rb'
require 'tempfile'

include ReferenceApi
include Clients
include Models

describe ReferenceApi do

  port = (ENV['PORT'] || raise("port is required")).to_i
  client = Client.new("http://localhost:#{port}")
  puts "PORT -> #{port}"

  describe Organizations do
    it "should work" do
      organization = make_organization
      client.organizations.post(organization)
      client.organizations.get_by_guid(organization.guid).should == organization
      client.organizations.get(:guid => organization.guid).should == [organization]

      client.organizations.get(:name => organization.name).each { |o|
        o.name.should == organization.name
      }

      client.organizations.get(:name => "blah").should be_empty
    end
  end

  describe Users do
    it "should default to creating active users" do
      user_form = make_user_form
      client.users.post(user_form).should be_active
    end

    it "should create active users" do
      user_form = make_user_form
      client.users.post(user_form, :active => true).should be_active
    end

    it "should create inactive users" do
      user_form = make_user_form
      client.users.post(user_form, :active => false).should_not be_active
    end

    it "should post profiles" do
      user_form = make_user_form
      user = client.users.post(user_form)
      file = Tempfile.new(user.guid + "-profile")
      file.puts "Sagitarius"
      file.close
      client.users.post_profile_by_guid(user.guid, File.new(file.path))
    end

    it "should work" do
      user_form = make_user_form
      user = client.users.post(user_form)
      user.email.should == user_form.email

      client.users.get(:guid => user.guid, :active => true).should == [user]
      client.users.get(:guid => user.guid, :active => false).should be_empty
      client.users.get(:email => user.email, :active => true).should == [user]
      client.users.get(:email => user.email, :active => false).should be_empty

      begin
        us = client.users.get(:active => true)
        us.each { |u| u.should be_active }
        us.should include(user)
      end

      begin
        us = client.users.get(:active => false)
        us.each { |u| u.should_not be_active }
        us.should_not include(user)
      end
    end
  end

  describe Members do
    it "should work" do
      user_form = make_user_form
      organization = make_organization
      client.organizations.post(organization).should_not be_nil
      user = client.users.post(user_form)
      user.should_not be_nil
      role = Faker::Lorem.word

      member_form = MemberForm.new(
        :organization => organization.guid,
        :user => user.guid,
        :role => role)
      member = client.members.post(member_form)
      member.organization.should == organization
      member.user.should == user
      member.role.should == role

      client.members.get(:guid => member.guid).should == [member]
      client.members.get(:organization => organization.guid).should == [member]
      client.members.get(:user => user.guid).should == [member]
      client.members.get(:role => role).should == [member]
      client.members.get_by_organization(organization.guid).should == [member]
    end
  end
end
