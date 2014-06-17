Deploying a schema change
=========================
Log into EC2 instance in security group w/ access to the database
(e.g. an API instance)

  sudo apt-get install postgresql-client
  sudo apt-get install git
  sudo apt-get install ruby

  git clone git://github.com/gilt/schema-evolution-manager.git
  cd schema-evolution-manager
  git checkout 0.9.12
  ruby ./configure.rb
  sudo ruby ./install.rb