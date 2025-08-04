from flask import Flask, jsonify
import docker

app = Flask(__name__)
client = docker.from_env()

@app.route("/")
def home():
    return "Hello from Flask root!"
	
@app.route('/containers')
def list_containers():
    containers = client.containers.list()
    container_list = [{'id': c.id[:12], 'name': c.name, 'status': c.status} for c in containers]
    return jsonify(container_list)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
