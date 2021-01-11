#!/bin/bash

echo "user1_viewer, service1 => expected 403 Forbidden"
curl -vv -H "Authorization: Bearer user1_viewer" -w "\n"  http://localhost:18000/service/1
echo "user2_editor, service1 => expected 200 Ok"
curl -vv -H "Authorization: Bearer user2_editor" -w "\n"  http://localhost:18000/service/1
echo "user3_owner, service1 => expected 200 Ok"
curl -vv -H "Authorization: Bearer user3_owner" -w "\n"  http://localhost:18000/service/1
