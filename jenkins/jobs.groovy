pipelineJob('Build_and_Push_Flask_App') {
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

  environment {
    DOCKERHUB_CREDENTIALS = 'dockerhub-credentials-id'
    DOCKERHUB_REPO = 'maoriiko/flask-app'
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

    stage('Build Docker Image') {
      steps {
        container('docker') {
          sh 'docker build -t \$DOCKERHUB_REPO:latest ./flask-app'
        }
      }
    }

    stage('Login to DockerHub') {
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            sh 'echo \$PASS | docker login -u \$USER --password-stdin'
          }
        }
      }
    }

    stage('Push Image to DockerHub') {
      steps {
        container('docker') {
          sh 'docker push \$DOCKERHUB_REPO:latest'
        }
      }
    }
  }
}
      """.stripIndent())
      sandbox()
    }
  }
}

pipelineJob('Build_and_Push_Nginx_Proxy') {
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

  environment {
    DOCKERHUB_CREDENTIALS = 'dockerhub-credentials-id'
    DOCKERHUB_REPO = 'maoriiko/nginx-proxy'
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

    stage('Prepare Nginx Config') {
      steps {
        container('docker') {
          sh '''
            cp ./nginx-proxy/nginx.conf ./nginx-proxy/nginx.conf.modified
            sed -i "/location \\\//a \\
                proxy_set_header X-Forwarded-For \$remote_addr; \\
                proxy_pass http://flask-app:5000;" ./nginx-proxy/nginx.conf.modified
          '''
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        container('docker') {
          sh '''
            docker build -t \$DOCKERHUB_REPO:latest -f ./nginx-proxy/Dockerfile ./nginx-proxy
          '''
        }
      }
    }

    stage('Login to DockerHub') {
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            sh 'echo \$PASS | docker login -u \$USER --password-stdin'
          }
        }
      }
    }

    stage('Push Image to DockerHub') {
      steps {
        container('docker') {
          sh 'docker push \$DOCKERHUB_REPO:latest'
        }
      }
    }
  }
}
      """.stripIndent())
      sandbox()
    }
  }
}

pipelineJob('Run_And_Test_Containers') {
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

  environment {
    DOCKERHUB_FLASK = 'maoriiko/flask-app:latest'
    DOCKERHUB_NGINX = 'maoriiko/nginx-proxy:latest'
    DOCKERHUB_CREDENTIALS = 'dockerhub-credentials-id'
  }

  stages {
    stage('Login to DockerHub') {
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDENTIALS, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            sh 'echo \$PASS | docker login -u \$USER --password-stdin'
          }
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

    stage('Cleanup') {
      steps {
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
}
      """.stripIndent())
      sandbox()
    }
  }
}
