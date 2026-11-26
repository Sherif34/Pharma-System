pipeline {
    agent { label 'linux' }

    tools {
        jdk 'JDK 17'
        maven 'Maven 3'
    }

    stages {
        stage('Java tests and coverage') {
            steps {
                sh '''
                    status=0
                    for service in catalogue inventory purchasing; do
                        mvn --batch-mode --file "services/${service}-service/java-tests/pom.xml" clean verify || status=1
                    done
                    exit "$status"
                '''
            }
            post {
                always {
                    junit testResults: 'services/*/java-tests/target/surefire-reports/TEST-*.xml',
                          allowEmptyResults: true
                    archiveArtifacts artifacts: 'services/*/java-tests/target/site/jacoco/**',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube Cloud quality gate') {
            when {
                anyOf {
                    branch 'main'
                    allOf {
                        changeRequest target: 'main'
                        expression { !env.CHANGE_FORK }
                    }
                }
            }
            environment {
                SONAR_TOKEN = credentials('sonar-token')
            }
            steps {
                dir('services/catalogue-service/java-tests') {
                    sh '''
                        if [ -n "${CHANGE_ID:-}" ]; then
                            set -- "-Dsonar.pullrequest.key=$CHANGE_ID" \
                                "-Dsonar.pullrequest.branch=$CHANGE_BRANCH" \
                                "-Dsonar.pullrequest.base=$CHANGE_TARGET"
                        else
                            set --
                        fi

                        mvn --batch-mode \
                            org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar \
                            -Dsonar.projectKey=Sherif34_Pharma-System \
                            -Dsonar.organization=sherif34 \
                            -Dsonar.host.url=https://sonarcloud.io \
                            -Dsonar.qualitygate.wait=true "$@"
                    '''
                }
            }
        }
    }
}
