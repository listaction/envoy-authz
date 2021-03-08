## Envoy w/ external auth example

### Quick Start
```
./demo.sh
```
(builds authz service, demo service (which is behind envoy+authz) , starts envoy which connects to authz and demoservice and runs test.py which validates acls for routes and objects created by the demo service) 
