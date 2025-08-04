# 🚀 DevOps & MLOps Assignment Project

This repository contains a complete CI/CD pipeline using Jenkins, Docker, Kubernetes, and KEDA for a Flask-based web application.

---

## 🧩 Project Structure

```
.
├── flask-app/              # Python Flask app that lists local Docker containers
├── nginx-proxy/           # Nginx container configured as reverse proxy
├── k8s/                   # Kubernetes manifests
├── jenkins-jobs/          # Groovy DSL files to create Jenkins jobs
└── README.md
```

---

## 📦 Technologies

- Jenkins (Pipeline + Job DSL Plugin)
- Docker & Docker Hub
- Python (Flask)
- Nginx
- Kubernetes (k8s)
- KEDA (Kubernetes Event-driven Autoscaler)
- GitHub

---

## 🔧 Jenkins Pipelines

### 🧪 1. Flask App Pipeline

- Pulls code from GitHub
- Builds Docker image of Flask app
- Pushes image to Docker Hub

### 🌐 2. Nginx Proxy Pipeline

- Builds a default Nginx container
- Injects a reverse proxy config with request header (source IP)
- Pushes image to Docker Hub

### 🚦 3. Runtime Job

- Runs both containers on Jenkins host
- Exposes Nginx only
- Sends test request to validate correct forwarding to Flask app

---

## ☸️ Kubernetes Deployment

- Flask app runs with PersistentVolumeClaim
- KEDA is deployed and configured to autoscale based on metrics
- Includes services, deployments, and configs for both Flask and Nginx apps

---

## 🧪 Test Instructions

You can test the full flow locally or in a Kubernetes environment.

1. Trigger Jenkins pipeline to build and push both images.
2. Deploy Kubernetes manifests from `k8s/` folder.
3. Validate:
   - Flask app responds with running Docker containers.
   - Nginx proxies to Flask correctly and adds headers.
   - KEDA scales the app on trigger.

---

## 💡 Notes

- All Docker images are pushed to Docker Hub (replace with your repo)
- Jenkins setup assumes Job DSL Plugin is pre-installed
- Make sure KEDA CRDs are installed before applying manifests

---

## 📁 Files Worth Noting

- `flask-app/app.py` – added default route `/`
- `nginx-proxy/nginx.conf` – updated proxy config
- `k8s/keda-scaledobject.yaml` – KEDA configuration added

---

## ✍️ Author

**Maoriiko** – [GitHub](https://github.com/Maoriiko)

---

## 📄 License

MIT License – free to use, modify, and share with attribution.
