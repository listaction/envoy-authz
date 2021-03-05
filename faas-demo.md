# FaaS Demo

Build artifact:
```bash
mvn clean package -DskipTests=true --projects auth --also-make
```

## Local

Run pods locally:
```bash
skaffold run

while [[ $(kubectl get pods -l app=cassandra -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}') != "True" ]]; do echo "waiting for pod" && sleep 1; done

kubectl exec cassandra -- cqlsh cassandra -u cassandra -p cassandra -e "CREATE KEYSPACE authz WITH replication= {'class':'SimpleStrategy', 'replication_factor':1};"

```

When done, delete local pods:
```bash
skaffold delete
```

## Share

Build and push Docker image to share with team:
```bash
DOCKER_IMAGE=docker.io/kettil/faas-authserver
docker build -f Dockerfile -t "$DOCKER_IMAGE" .
docker push "$DOCKER_IMAGE"

```
