#!groovy

def appName = 'maven-ola'
def builderImage = 'maven:latest'

node () {
    def builder = docker.image(builderImage)
    builder.pull() // make sure we have the latest available from Docker Hub

    stage ('Checkout'){
        echo 'Checking out git repository'
        checkout scm
    }

    builder.inside {
        stage ('Compile') {
            sh "mvn -Dmaven.repo.local=${pwd tmp: true}/m2repo \
                compile"
        }

        stage ('Build Package'){
            sh "mvn -Dmaven.repo.local=${pwd tmp: true}/m2repo \
                package"
        }

        stage ('Test Package'){
            sh "mvn -Dmaven.repo.local=${pwd tmp: true}/m2repo \
                -B -Dmaven.test.failure.ignore verify"
        }

        stage ('Save Package'){
            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        }
    }
}
