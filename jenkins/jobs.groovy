pipelineJob('Build_and_Run_Flask_Nginx') {
  definition {
    cps {
      script("""
pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps {
        git url: 'https://github.com/yourusername/yourrepo.git', branch: 'main'
      }
    }
    
    stage('Build Flask Image') {
      steps {
        script {
          docker.build('flask-app:latest', './flask-app')
        }
      }
    }
    
    stage('Build Nginx Image') {
      steps {
        script {
          docker.build('nginx-proxy:latest', './nginx-proxy')
        }
      }
    }
    
    stage('Run Containers') {
      steps {
        script {
          sh '''
            docker network create flask-net || true
            docker run -d --rm --name flask-app --network flask-net flask-app:latest
            docker run -d --rm --name nginx-proxy -p 8081:80 --network flask-net nginx-proxy:latest
            sleep 10
            curl -f http://localhost:8081/containers
          '''
        }
      }
    }
  }
  
  post {
    always {
      sh '''
        docker stop flask-app || true
        docker stop nginx-proxy || true
        docker network rm flask-net || true
      '''
    }
  }
}
      """.stripIndent())
      sandbox()
    }
  }
}
