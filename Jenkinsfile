properties([pipelineTriggers([githubPush()])])

pipeline {
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 30, unit: 'MINUTES')
  }

  agent {
    kubernetes {
      label 'worker-apibuilder'
      inheritFrom 'default'

      containerTemplates([
        containerTemplate(name: 'helm', image: "flowcommerce/k8s-build-helm2:0.0.50", command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'docker', image: 'docker:18', resourceRequestCpu: '1', resourceRequestMemory: '2Gi', command: 'cat', ttyEnabled: true)
      ])
    }
  }

  environment {
    ORG      = 'flowcommerce'
  }

  stages {
    stage('Checkout') {
      steps {
        checkoutWithTags scm

        script {
          VERSION = new flowSemver().calculateSemver() //requires checkout
        }
      }
    }

    stage('Commit SemVer tag') {
      when { branch 'main' }
      steps {
        script {
          new flowSemver().commitSemver(VERSION)
        }
      }
    }

    stage('Build and push docker image release') {
      when { branch 'main' }
      steps {
        container('docker') {
          script {
            semver = VERSION.printable()
            
            docker.withRegistry('https://index.docker.io/v1/', 'jenkins-dockerhub') {
              db = docker.build("$ORG/apibuilder-api:$semver", '--network=host -f api/Dockerfile .')
              db.push()
            }
            
            docker.withRegistry('https://index.docker.io/v1/', 'jenkins-dockerhub') {
              db = docker.build("$ORG/apibuilder-app:$semver", '--network=host -f app/Dockerfile .')
              db.push()
            }
            
          }
        }
      }
    }

    stage('Display Helm Diff') {
      when {
        allOf {
          not {branch 'main'}
          changeRequest()
          expression {
            return changesCheck.hasChangesInDir('deploy')
          }
        }
      }
      steps {
        script {
          container('helm') {
            if(changesCheck.hasChangesInDir('deploy/apibuilder-api')){
              new helmDiff().diff('apibuilder-api')
            }
            if(changesCheck.hasChangesInDir('deploy/apibuilder-app')){
              new helmDiff().diff('apibuilder-app')
            }
          }
        }
      }
    }

    stage('Deploy Helm chart') {
      when { branch 'main' }
      parallel {
        
        stage('deploy apibuilder-api') {
          steps {
            script {
              container('helm') {
                new helmCommonDeploy().deploy('apibuilder-api', 'apicollective', VERSION.printable(), 300)
              }
            }
          }
        }
        
        stage('deploy apibuilder-app') {
          steps {
            script {
              container('helm') {
                new helmCommonDeploy().deploy('apibuilder-app', 'apicollective', VERSION.printable(), 300)
              }
            }
          }
        }
        
      }
    }
  }
}
