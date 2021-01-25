#!/bin/bash
docker-compose down
mvn clean install
docker-compose build && docker-compose up -d
sleep 15
python3 test.py
