def call(Map configMap){
    pipeline {
    agent {
        label 'agent-1'
    }
    // agent any
    environment {
        packageversion=''
        nexusURL='54.83.239.184:8081'
    }
    parameters {
        booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
    }

    options {
        timeout(time: 1, unit: 'HOURS') // Set a timeout for the entire pipeline
        ansiColor("xterm") 
        disableConcurrentBuilds()
    }

    stages {
        stage('Get version from Json file') {
            steps {
                script {
                    def jsonData = readJSON file: 'package.json'
                    packageversion=jsonData.version
                    echo "version : $packageversion"
                }
            }
        }

        stage('install dependencies') {
            steps {
                sh"""
                    npm install
                """
            }
        }

        stage('Sonar Scan'){
            steps{
                sh """
                    echo "sonar-scanner"  
                """
            }
        }

        stage('Unit testing'){
            steps{
                sh """
                    echo "Unit tests will run here"
                """
            }
        }

        stage('Build artifact') {
            steps {
                sh """
                    ls -ltr
                    zip -q -r catalogue.zip . -x "*.zip" -x ".git/*"
                    ls -ltr 
                """
            }
        }

        stage('Publish artifact') {
            steps {
                script {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: "$nexusURL",
                        groupId: 'com.roboshop',
                        version: "$packageversion",
                        repository: 'catalogue',
                        credentialsId: 'nexus-auth',
                        artifacts: [
                            [artifactId: 'catalogue',
                             classifier: '',
                             file: 'catalogue.zip',
                             type: 'zip']
                        ]
                    )
                }
            }
        }

        stage('Deploy') {
            when {
                // Execute the deployment stage only if the 'deploy' parameter is set to true
                expression {
                    params.deploy == true
                }
            }
            steps {
                sh"""
                echo "$pwd"
                """


                script {
                    echo "$pwd"
                    build job: 'catalogue-deploy',parameters: [
                        // Pass parameters to the downstream job
                        string(name: 'packageversion', value: "$packageversion"),
                        // Add more parameters as needed
                    ]
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline completed successfully. Sending notification..."
            // Add notification steps for successful builds
        }

        failure {
            echo "Pipeline failed. Sending notification..."
            // Add notification steps for failed builds
        }

        always {
            echo "Cleaning up..."
            // Add any cleanup steps that should run regardless of success or failure
            deleteDir()
        }
    }
}
}