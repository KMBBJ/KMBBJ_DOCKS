pipeline {
    agent any // 모든 사용 가능한 에이전트(또는 노드)에서 파이프라인을 실행
    environment {
        GITHUB_TOKEN = credentials('github-token') // 설정한 Credential ID 사용
    }
    stages {
        stage('Checkout') { // 첫 번째 단계: 코드 체크아웃
            steps {
                // Git 리포지토리에서 코드 체크아웃
                // - branch: 'main' 브랜치를 체크아웃
                // - url: Git 리포지토리 URL
                // - credentialsId: 인증 정보 ID를 사용하여 리포지토리에 접근
                git branch: 'main', url: 'https://github.com/KMBBJ/KMBBJ_BACKEND.git', credentialsId: 'parkswon1'
            }
        }
        stage('Fetch and Create .env') {
            steps {
                script {
                    sh 'mkdir -p backend/src/main/resources/properties'
                    def envContent = sh(script: "curl -H 'Authorization: token ${GITHUB_TOKEN}' https://api.github.com/repos/your-repo/your-repo-name/actions/secrets/ENV", returnStdout: true).trim()
                    writeFile file: 'backend/src/main/resources/properties/.env', text: envContent
                }
            }
        }
        stage('Build') { // 두 번째 단계: 코드 빌드
            steps {
                // 빌드 단계 로그 메시지 출력
                echo 'Building...'
                sh 'echo $JAVA_HOME'
                sh 'java -version'
                sh 'chmod 755 backend/gradlew'
                sh 'cd backend && ./gradlew build'
                // 여기에 실제 빌드 작업을 추가
            }
        }

        stage('Test') { // 세 번째 단계: 테스트
            steps {
                // 테스트 단계 로그 메시지 출력
                echo 'Testing...'
                // 여기에 실제 테스트 작업을 추가
            }
        }

        stage('Deploy') { // 네 번째 단계: 배포
            steps {
                // 배포 단계 로그 메시지 출력
                echo 'Deploying...'
                // 여기에 실제 배포 작업을 추가
            }
        }
    }
}