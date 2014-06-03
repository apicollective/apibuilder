require 'spec_helper.rb'

# TODO finish the ruby Reference API spec

include ReferenceApi
include Clients

describe ReferenceApi do

  port = (ENV['PORT'] || raise("port is required")).to_i
  client = Client.new("http://localhost:#{port}")
  puts "PORT -> #{port}"

  describe Organizations do
    it "should work" do
      organization = make_organization
      client.organizations.post(organization.to_h)

      begin
        o = client.organizations.get_by_guid(organization.guid)
        # ruby gem doesn't generate an == method :(
        o.guid.should == organization.guid
        o.name.should == organization.name
      end

      begin
        os = client.organizations.get(:guid => organization.guid)
        os.each { |o|
          o.guid.should == organization.guid
        }
        os.size.should == 1
      end

      client.organizations.get(:name => organization.name).each { |o|
        o.name.should == organization.name
      }
    end
  end

  describe Users do
    it "should work" do
      user = make_user
      client.users.post(user.to_h)
    end
  end

  describe Members do
    it "should work" do
      member = make_member
      client.members.post(member.to_h)
    end
  end
end
