pipelineJob('Build_and_Run_Flask_Nginx') {
  definition {
    cps {
      script("""
pipeline {
  agent {
    kubernetes {
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: docker:20.10.16-dind
    securityContext:
      privileged: true
'''
    }
  }

  stages {
    stage('Checkout') {
      steps {
        checkout([
          \$class: 'GitSCM',
          branches: [[name: '*/main']],
          userRemoteConfigs: [[
            url: 'https://github.com/Maoriiko/HomeWork.git',
            credentialsId: 'GithubToken2'
          ]]
        ])
      }
    }

    stage('Build Flask Image') {
      steps {
        container('docker') {
          sh 'docker build -t flask-app:latest ./flask-app'
        }
      }
    }

    stage('Build Nginx Image') {
      steps {
        container('docker') {
          sh 'docker build -t nginx-proxy:latest ./nginx-proxy'
        }
      }
    }

    stage('Run Containers') {
      steps {
        container('docker') {
          sh '''
            docker network create flask-net || true
            docker run -d --rm --name flask-app --network flask-net flask-app:latest
            docker run -d --rm --name nginx-proxy -p 8081:80 --network flask-net nginx-proxy:latest

            echo "--- Inspecting docker network flask-net ---"
            docker network inspect flask-net

            echo "--- Checking running containers ---"
            docker ps

            echo "--- Trying ping from another container ---"
            docker run --rm --network flask-net busybox ping -c 3 nginx-proxy || true

            echo "--- Curl using direct IP ---"
            nginx_ip=\\$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' nginx-proxy)
            echo "NGINX IP: \\$nginx_ip"
            docker run --rm --network flask-net curlimages/curl -v http://\\$nginx_ip/containers
          '''
        }
      }
    }
  }

  post {
    always {
      container('docker') {
        sh '''
          docker stop flask-app || true
          docker stop nginx-proxy || true
          docker network rm flask-net || true
        '''
      }
    }
  }
}
      """.stripIndent())
      sandbox()
    }
  }
}
