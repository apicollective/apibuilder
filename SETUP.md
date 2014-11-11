Deploying a schema change
=========================
Log into EC2 instance in security group w/ access to the database
(e.g. an API instance)

  sudo apt-get -y install postgresql-client git ruby

  git clone git://github.com/gilt/schema-evolution-manager.git
  cd schema-evolution-manager
  git checkout 0.9.12
  ruby ./configure.rb
  sudo ruby ./install.rb

  echo "apidoc2.cqe9ob8rnh0u.us-east-1.rds.amazonaws.com:5432:apidoc:web:PASSWORD" > ~/.pgpass
  chmod 0600 ~/.pgpass

  sem-apply --host apidoc2.cqe9ob8rnh0u.us-east-1.rds.amazonaws.com --name apidoc --user web