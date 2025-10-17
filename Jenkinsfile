@Library('lib-jenkins-pipeline') _


//  => Mandatory to use a definition before the node or Blue Ocean doesn't show the expected info
def newTagEveryRunMainBranch = "yes" // Force a new version and deploy clicking on Build Now in Jenkins
def sbtOnMain = "no"

// we can remove the pod_template block if we end up having only one template
// in jenkins config
//
String podLabel = "Jenkinsfile-apibuilder"
podTemplate(
  label: "${podLabel}",
  inheritFrom : 'generic'
){
  node(podLabel) {
    try {
      checkoutWithTags scm
      //Checkout the code from the repository
      stage('Checkout') {
          echo "Checking out branch: ${env.BRANCH_NAME}"
          checkout scm
      }

      // => tagging function to identify what actions to take depending on the nature of the changes
      stage ('tagging') {
        semversion = taggingv2(newTagEveryMainRun: "${newTagEveryRunMainBranch}")
        println(semversion)
      }

      // => Running the actions for each component in parallel
      checkoutWithTags scm

      String jsondata = '''
      [{"serviceName": "apibuilder-api",
      "dockerImageName": "apibuilder-api",
      "dockerFilePath" : "/api/Dockerfile",
      "multiplatforms: "no"},
      {"serviceName": "apibuilder-app",
      "dockerImageName": "apibuilder-app",
      "dockerFilePath" : "/app/Dockerfile",
      "multiplatforms: "no"}]
      '''
      withCredentials([string(credentialsId: "jenkins-argocd-token", variable: 'ARGOCD_AUTH_TOKEN')]) {
        mainJenkinsBuildArgo(
          semversion: "${semversion}",
          pgImage: "flowcommerce/bentest-postgresql:latest",
          componentargs: "${jsondata}",
          sbtOnMain: "${sbtOnMain}"
          // => optional
          //orgName: "flowvault"
          // SBT test
          //sbtCommand: 'sbt clean flowLint coverage test scalafmtSbtCheck scalafmtCheck doc',
          //playCpuLimit: "2",
          //playMemoryRequest: "4Gi",
          //pgCpuLimit: "1",
          //pgMemoryRequest: "2Gi",
          //sbtTestInMain: "${sbtOnMain}"
        )
      }

    } catch (Exception e) {
        // In case of an error, mark the build as failure
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        // Always clean up workspace and notify if needed
        cleanWs()
        echo "Pipeline execution finished"
    }
  }
}

