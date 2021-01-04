#!/bin/bash

curl -vv -H "Authorization: Bearer foo" -w "\n"  http://localhost:18000/service/1
curl -vv -H "Authorization: Bearer bar" -w "\n"  http://localhost:18000/service/1
