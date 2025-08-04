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
			echo "--- Creating docker network ---"
			docker network create flask-net || true

			echo "--- Running flask-app ---"
			docker run -d --rm --name flask-app \\
			  -p 5001:5000 \\
			  --network flask-net \\
			  -v /var/run/docker.sock:/var/run/docker.sock \\
			  flask-app:latest

			echo "--- Waiting for Flask to be ready ---"
			sleep 5

			echo "--- Running nginx-proxy ---"
			docker run -d --rm --name nginx-proxy \\
			  -p 8081:80 \\
			  --network flask-net \\
			  -v /var/run/docker.sock:/var/run/docker.sock \\
			  nginx-proxy:latest

			echo "--- Checking running containers ---"
			docker ps

			echo "--- Curl localhost through nginx ---"
			curl -v http://localhost:8081/containers || true
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
