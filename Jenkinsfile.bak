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
      inheritFrom 'kaniko-slim'
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
      parallel {
        stage('API builder api') {
          steps {
            container('kaniko') {
              script {
                semver = VERSION.printable()

                sh """
                  /kaniko/executor -f `pwd`/api/Dockerfile -c `pwd` \
                  --snapshot-mode=redo --use-new-run  \
                  --destination ${env.ORG}/apibuilder-api:$semver
                """             
              }
            }
          }
        }

        stage('API builder app') {
          agent {
            kubernetes {
              label 'worker-apibuilder-app'
              inheritFrom 'kaniko-slim'
            }
          }
          steps {
            container('kaniko') {
              script {
                semver = VERSION.printable()

                sh """
                  /kaniko/executor -f `pwd`/app/Dockerfile -c `pwd` \
                  --snapshot-mode=redo --use-new-run  \
                  --destination ${env.ORG}/apibuilder-app:$semver
                """                    
              }
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
