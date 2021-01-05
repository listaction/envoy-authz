#!/bin/bash

echo "foo, service1 => must be 200 OK"
curl -vv -H "Authorization: Bearer foo" -w "\n"  http://localhost:18000/service/1
echo "bar, service1 => must be 403 Forbidden"
curl -vv -H "Authorization: Bearer bar" -w "\n"  http://localhost:18000/service/1

echo "foo, service2 => must be 403 Forbidden"
curl -vv -H "Authorization: Bearer foo" -w "\n"  http://localhost:18000/service/2
echo "bar, service2 => must be 200 OK"
curl -vv -H "Authorization: Bearer bar" -w "\n"  http://localhost:18000/service/2
