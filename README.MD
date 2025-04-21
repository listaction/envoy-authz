# Envoy External Authorization (AuthZ) Demo

This repository demonstrates a production-grade implementation of external authorization (AuthZ) using Envoy Proxy, with a complete microservices architecture showcasing access control and authentication.

This implementation is based on Google's Zanzibar paper, which describes a globally distributed authorization system used by Google to handle authorization for services like Google Drive, Calendar, and Cloud Platform. The paper can be found here: [Google Zanzibar Paper](https://research.google/pubs/pub48190/)

## Zanzibar Architecture Overview

The Zanzibar architecture consists of several key components:

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Service                          │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Envoy Proxy                             │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                          AuthZ Service                           │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Storage Layer                           │
└─────────────────────────────────────────────────────────────────┘
```

### Key Concepts from Zanzibar

1. **Relation Tuples**
   - Objects are identified by their namespace and ID
   - Relations define how objects are connected
   - Example: `document:readme#owner@user:alice`

2. **Consistency Model**
   - Strong consistency for writes
   - Eventual consistency for writes (this is different from the original Zanzibar Implementation )
   - Global snapshot reads

3. **Performance Characteristics**
   - 95th percentile latency < 10ms
   - 99th percentile latency < 100ms
   - Handles millions of QPS

### Zanzibar-inspired Features in this Implementation

1. **Namespace-based Authorization**
   ```
   namespace:object#relation@user
   └─────────┴─────┴────────┴────┘
      │        │       │      │
      │        │       │      └─ Subject (user or group)
      │        │       └─ Relation (permission)
      │        └─ Object ID
      └─ Namespace
   ```

2. **Relation-based Access Control**
   ```
   ┌─────────────────────────────────────────────────────────────┐
   │                     Access Control Graph                     │
   │                                                             │
   │  document:readme ────── owner ──────► user:alice            │
   │         │                                                     │
   │         └─────── reader ───────► group:engineering           │
   │                                                             │
   └─────────────────────────────────────────────────────────────┘
   ```

3. **Consistency Model**
   ```
   ┌─────────────────────────────────────────────────────────────┐
   │                     Consistency Model                        │
   │                                                             │
   │  Write ──────► Strong Consistency ──────► Global State      │
   │                                                             │
   │  Read  ──────► Eventual Consistency ────► Local Cache       │
   │                                                             │
   └─────────────────────────────────────────────────────────────┘
   ```

## Architecture Overview

The system consists of the following components:

- **Front Envoy**: Acts as the API Gateway/Edge Proxy
- **AuthZ Service**: Handles authorization decisions
- **Demo Service**: Example backend service with protected endpoints
- **Redis**: For caching and session management
- **PostgreSQL**: Persistent storage for authorization data

### System Components

1. **Front Envoy (Port 18000)**
   - Main entry point for all API requests
   - Implements external authorization filter
   - Routes traffic to appropriate services
   - Admin interface available on port 8001

2. **AuthZ Service (Port 8080, 8081)**
   - Implements authorization logic
   - Provides ACL management API
   - Integrates with Redis for caching
   - Uses PostgreSQL for persistent storage

3. **Demo Service (Port 8002)**
   - Example protected service
   - Demonstrates integration with the authorization system
   - Provides sample protected endpoints

4. **Supporting Infrastructure**
   - Redis (Port 6379): For caching and session management
   - PostgreSQL (Port 5432): For persistent storage

## Getting Started

### Prerequisites

- Docker
- Docker Compose
- Java Development Kit (JDK) 11 or higher (for development)

### Quick Start

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd envoy-authz
   ```

2. Start the services:
   ```bash
   docker-compose up -d
   ```

3. Verify the setup:
   ```bash
   curl localhost:18000/contact/
   ```

### Authorization Setup

The system supports both coarse-grained and fine-grained authorization. Here's how to set up different authorization levels:

#### 1. Coarse-grained Authorization (Role-based)

Create roles and assign them to users:

```bash
# Create a new role
curl -X POST http://localhost:8081/acl/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "admin",
    "permissions": ["read", "write", "delete"]
  }'

# Assign role to user
curl -X POST http://localhost:8081/acl/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "roles": ["admin"]
  }'
```

#### 2. Fine-grained Authorization (Resource-based)

Set up specific permissions for resources:

```bash
# Create a resource with specific permissions
curl -X POST http://localhost:8081/acl/resources \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "contact",
    "resourceId": "contact123",
    "permissions": {
      "user123": ["read", "write"],
      "user456": ["read"]
    }
  }'
```

#### 3. Testing Authorization

Verify authorization rules:

```bash
# Test coarse-grained access
curl -X GET http://localhost:18000/contact/ \
  -H "X-User-ID: user123"

# Test fine-grained access
curl -X GET http://localhost:18000/contact/contact123 \
  -H "X-User-ID: user456"
```

#### 4. Common Authorization Patterns

1. **Role-based Access Control (RBAC)**
   - Define roles (admin, user, guest)
   - Assign permissions to roles
   - Assign roles to users

2. **Attribute-based Access Control (ABAC)**
   - Define attributes (department, location)
   - Create policies based on attributes
   - Apply policies to resources

3. **Resource-based Access Control**
   - Define resource types
   - Set permissions per resource
   - Apply inheritance rules

Example RBAC setup:
```bash
# Create roles
curl -X POST http://localhost:8081/acl/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "admin",
    "permissions": ["*"]
  }'

curl -X POST http://localhost:8081/acl/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "user",
    "permissions": ["read", "write"]
  }'

# Assign roles to users
curl -X POST http://localhost:8081/acl/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "admin1",
    "roles": ["admin"]
  }'

curl -X POST http://localhost:8081/acl/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user1",
    "roles": ["user"]
  }'
```

### Development Setup

For local development:

1. Install dependencies:
   ```bash
   ./demo.sh setup
   ```

2. Run tests:
   ```bash
   ./demo.sh test
   ```

## Configuration

### Envoy Configuration

The main Envoy configuration is in `front-envoy.yaml`. Key features:
- External authorization filter configuration
- Route configurations for different services
- Security settings and timeouts

### Authorization Rules

Authorization rules can be configured through:
1. ACL API endpoints
2. Direct database configuration
3. Environment-specific configuration files

## API Documentation

### Protected Endpoints

1. Contact API (`/contact/`)
   - Requires authentication
   - Supports CRUD operations
   - Namespace: "contact"

2. ACL API (`/acl/`)
   - Administrative endpoints
   - Manages access control rules
   - Namespace: "acl"

## Testing

The repository includes several test suites:
- Unit tests
- Integration tests
- Performance tests

Run tests using:
```bash
./demo.sh test
```

## Performance Considerations

- Redis caching improves authorization decision performance
- Envoy's connection pooling handles high concurrency
- Configurable timeouts and circuit breakers

## Security

- All services run in isolated containers
- PostgreSQL credentials should be changed in production
- Redis security should be configured for production use
- TLS should be enabled in production deployments

## Contributing

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

See [License.md](License.md) for details.

## Troubleshooting

Common issues and solutions:

1. Connection refused:
   - Ensure all containers are running
   - Check port mappings

2. Authorization failures:
   - Verify Redis connection
   - Check PostgreSQL connectivity
   - Review authorization logs

## Contact

For issues and support, please create an issue in the repository.
