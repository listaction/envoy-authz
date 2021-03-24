#!/bin/bash
docker-compose down
mvn clean install
docker-compose build
docker-compose up -d cassandra
docker-compose up -d redis
sleep 30
docker run -it --network envoymesh --rm bitnami/cassandra:latest cqlsh cassandra -u cassandra -p cassandra -e "CREATE KEYSPACE IF NOT EXISTS authz WITH replication= {'class':'SimpleStrategy', 'replication_factor':1};"
docker-compose up -d
sleep 15

INGRESS=$(minikube service invoker --url)
if [[ $INGRESS != http://* ]]; then
  INGRESS=http://localhost:18000
fi

python3 test.py $INGRESS

echo "NOTE: if all tests fail or do not proceed , it is likely that cassandra is not ready / slow - check the cassandra container logs and then try once it looks stable"
