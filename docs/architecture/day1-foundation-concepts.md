# Day 1 — Foundation & Platform: Concepts & Interview Guide

---

## 1. Multi-Module Maven Project

### What it is

A single root `pom.xml` that acts as the parent for multiple sub-projects (modules).
Each module has its own `pom.xml` that declares `<parent>` pointing to the root.

### How it works

```
backend/pom.xml              ← root (packaging: pom)
├── shared/common-core/      ← module 1
├── platform/config-server/  ← module 2
├── platform/discovery-server/
├── platform/api-gateway/
└── services/auth-service/
```

When you run `mvn install` at the root, Maven builds all modules in dependency order.

### Why we used it

- Change Spring Boot version in **one place** — all modules pick it up
- `common-core` is built first and available as a `.jar` for all services
- CI/CD can build the entire backend with a single command

### Interview Q&A

**Q: What is a BOM (Bill of Materials)?**

> A BOM is a special POM with `<packaging>pom</packaging>` that only declares
> `<dependencyManagement>` — no actual dependencies. You import it to inherit
> version numbers without inheriting actual dependencies.
> Example: `spring-boot-dependencies` and `spring-cloud-dependencies` are BOMs.

**Q: What is the difference between `<dependencies>` and `<dependencyManagement>`?**

> `<dependencyManagement>` only declares versions — modules must still explicitly
> declare the dependency to get it. `<dependencies>` in a parent POM are inherited
> by ALL child modules automatically.

**Q: Why use `<relativePath>` in the child POM parent tag?**

> It tells Maven where to find the parent POM locally without downloading from
> a repository. Without it, Maven looks in the local repo and remote repos first,
> which can cause version mismatches during development.

---

## 2. Spring Cloud Config Server

### What it is

A Spring Boot application annotated with `@EnableConfigServer` that serves
configuration files to other services over HTTP on port **8888**.

### How it works

```
auth-service starts
  → sends GET http://localhost:8888/auth-service/default
  → Config Server reads config-repo/auth-service.yaml
  → returns the YAML as JSON
  → auth-service loads DB URL, JWT secret etc. from response
```

The client side uses:

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
```

`optional:` means the service still starts if Config Server is unavailable (uses local defaults).

### Profiles

Config Server supports environment-specific files:

- `auth-service.yaml` → default (all environments)
- `auth-service-dev.yaml` → only when profile is `dev`
- `auth-service-prod.yaml` → only when profile is `prod`

### Why we used it

- No hardcoded DB URLs or secrets in service JARs
- Change a config value → all running services pick it up via `/actuator/refresh`
- In production: Config Server points to a **Git repo** — config changes go through PR review

### Interview Q&A

**Q: What is the difference between native and Git backend for Config Server?**

> **Native**: reads config files from the local filesystem or classpath.
> Used in development — no Git needed.
> **Git**: reads config files from a Git repository (GitHub, GitLab, Bitbucket).
> Used in production — config changes are version-controlled, auditable, and
> can be reviewed via pull requests.

**Q: How do you refresh config without restarting a service?**

> Add `spring-boot-starter-actuator` and expose the `/actuator/refresh` endpoint.
> Call `POST /actuator/refresh` on the service — it re-fetches config from
> Config Server and rebinds `@ConfigurationProperties` beans.
> For all services at once, use Spring Cloud Bus (Kafka/RabbitMQ) to broadcast
> the refresh event.

**Q: What is `@RefreshScope`?**

> A Spring scope that destroys and recreates a bean when a `/refresh` event fires.
> Use it on beans that read from `@Value` or `@ConfigurationProperties` so they
> pick up the new config values without a restart.

---

## 3. Service Discovery — Eureka

### What it is

A REST-based service registry where microservices **register themselves** on startup
and **discover** other services by name instead of IP/port.

### How it works

```
1. service starts → POST http://eureka:8761/eureka/apps/AUTH-SERVICE
   body: { host: "10.0.0.5", port: 8081, status: UP }

2. every 30s → sends heartbeat to stay registered

3. service B wants to call auth-service:
   → asks Eureka: GET /eureka/apps/AUTH-SERVICE
   → gets list of instances with IPs
   → picks one (round-robin) and calls it
```

The `lb://` prefix in Gateway routes triggers Eureka lookup:

```yaml
uri: lb://auth-service # lb = load balanced via Eureka
```

### Key annotations

| Annotation                       | Where                              | Effect                        |
| -------------------------------- | ---------------------------------- | ----------------------------- |
| `@EnableEurekaServer`            | Discovery Server main class        | Turns the app into a registry |
| `@EnableEurekaClient` (implicit) | Any service with eureka-client dep | Auto-registers on startup     |

### Why we used it

- Services scale up/down — new instances auto-register, dead ones auto-deregister
- No hardcoded URLs between services
- API Gateway uses it to load balance across multiple instances

### Interview Q&A

**Q: What is the difference between client-side and server-side load balancing?**

> **Server-side**: a dedicated load balancer (e.g. AWS ELB, Nginx) sits between
> caller and callee. The caller doesn't know about multiple instances.
> **Client-side** (what Eureka uses): the caller fetches the list of instances
> from Eureka and decides which one to call. Spring Cloud LoadBalancer does
> this using round-robin by default. No separate load balancer needed.

**Q: What is Eureka self-preservation mode?**

> If Eureka stops receiving heartbeats from >15% of services within 1 minute,
> it enters self-preservation mode and stops evicting instances — assuming a
> network partition, not actual failures. We disable it in dev
> (`enable-self-preservation: false`) to see failures immediately.

**Q: What replaces Eureka in Kubernetes?**

> Kubernetes has built-in DNS-based service discovery. Every Service gets a
> DNS name like `auth-service.default.svc.cluster.local`. You can use this
> instead of Eureka in K8s deployments. Many teams use Eureka for local dev
> and switch to K8s DNS in production.

**Q: What is the difference between Eureka, Consul, and Zookeeper?**

> All are service registries.
> **Eureka**: AP system (availability + partition tolerance) — prefers availability.
> Simple, built for microservices.
> **Consul**: CP system — prefers consistency. Has built-in health checking,
> KV store, and multi-datacenter support.
> **Zookeeper**: CP system — used by Kafka/Hadoop. Complex but very reliable.

---

## 4. Spring Cloud Gateway

### What it is

A **reactive** (WebFlux-based) API gateway that acts as the single entry point
for all client requests. Runs on port **8080**.

### How it works

```
Client → POST /api/v1/auth/login
  → Gateway matches route: Path=/api/v1/auth/**
  → Gateway looks up auth-service in Eureka → gets 10.0.0.5:8081
  → Gateway forwards request to http://10.0.0.5:8081/api/v1/auth/login
  → response flows back to client
```

### Key concepts

| Concept       | What it does                                                    |
| ------------- | --------------------------------------------------------------- |
| **Route**     | A rule: if request matches predicate → forward to URI           |
| **Predicate** | Condition to match (Path, Method, Header, QueryParam)           |
| **Filter**    | Modify request/response (add headers, strip prefix, rate limit) |

### Why reactive (WebFlux)?

The gateway handles thousands of concurrent connections. A traditional servlet-based
gateway would block a thread per request. WebFlux uses event loop + non-blocking I/O —
fewer threads handle more connections.

### Why we used it

- One place to handle: JWT validation, rate limiting, CORS, logging, tracing
- Routes are dynamically loaded from Config Server
- No need to configure CORS in every single service

### Interview Q&A

**Q: What is the difference between Spring Cloud Gateway and Zuul?**

> **Zuul 1**: Netflix's old gateway. Blocking/servlet-based. Simpler but less scalable.
> **Zuul 2**: Non-blocking but complex.
> **Spring Cloud Gateway**: built on Spring WebFlux + Reactor Netty.
> Officially recommended. Better performance, cleaner API.

**Q: How does rate limiting work in Gateway?**

> Uses Redis to store request counters per user/IP with a sliding window.
> The `RequestRateLimiter` filter checks Redis before forwarding — if the
> limit is exceeded it returns `429 Too Many Requests`.
> Redis is used because it's fast (in-memory) and shared across Gateway instances.

**Q: What is `StripPrefix` filter?**

> Removes path segments before forwarding. If `StripPrefix=1` and the
> incoming path is `/api/auth/login`, it becomes `/auth/login` when forwarded.
> Used when your service doesn't know its own prefix.

---

## 5. Common Core Library

### What it is

A shared JAR that every microservice depends on. Contains:

- `ApiResponse<T>` — standard response envelope
- `ErrorCode` enum — centralized error codes
- `BaseException`, `BusinessException`, `ResourceNotFoundException`
- `GlobalExceptionHandler` — `@ControllerAdvice` for all services
- `AuditEntity` — base JPA entity with audit fields

### The ApiResponse pattern

Every API returns the same shape:

```json
// Success
{ "success": true, "data": { "token": "..." }, "timestamp": "2026-07-16T..." }

// Error
{ "success": false, "error": { "code": "AUTH-001", "message": "Invalid credentials" }, "timestamp": "..." }
```

### Why centralized error codes?

- Frontend knows exactly which error code maps to which UI message
- Logs are searchable by code: `grep "AUTH-001" logs`
- Easy to add new codes without changing contracts

### Interview Q&A

**Q: What is `@ControllerAdvice` and how does exception handling work?**

> `@ControllerAdvice` is a specialization of `@Component` that acts as a
> cross-cutting concern for all `@Controller`s. When any controller throws
> an exception, Spring looks for a matching `@ExceptionHandler` method in
> `@ControllerAdvice` classes.
> Execution order: specific handler first → parent class handler → catch-all.

**Q: What is `@MappedSuperclass` in JPA?**

> Marks a class whose fields are mapped into the subclass's table — it does
> NOT create its own table. Used for `AuditEntity` so every entity table
> gets `created_at`, `updated_at` columns without code duplication.

**Q: What is `@EntityListeners(AuditingEntityListener.class)`?**

> Registers a JPA lifecycle callback. `AuditingEntityListener` automatically
> sets `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`
> fields before INSERT/UPDATE. You also need `@EnableJpaAuditing` on your
> Spring Boot app config.

---

## 6. Startup Order — Why It Matters

```
1. Config Server (8888)     — must be first; all others fetch config from it
2. Discovery Server (8761)  — fetches its own config, then accepts registrations
3. All business services    — fetch config → register with Eureka → ready
```

If a service starts before Config Server, Spring uses `optional:` fallback
(local `application.yaml` defaults). If it starts before Eureka, it retries
registration every 30 seconds automatically.

### Interview Q&A

**Q: How do you handle service startup ordering in production (Kubernetes)?**

> Kubernetes doesn't guarantee pod startup order. Use:
>
> - **Init containers**: a pod waits for a dependency to be healthy before starting
> - **Readiness probes**: service only receives traffic when `/actuator/health` returns UP
> - **Retry logic**: services retry Config Server and Eureka connections with
>   exponential backoff — eventually consistent startup

---

## Quick Reference — What Goes Where

| You want to...                              | Use                                                                  |
| ------------------------------------------- | -------------------------------------------------------------------- |
| Share response format across services       | `ApiResponse<T>` in common-core                                      |
| Add a new error type                        | Add entry to `ErrorCode` enum                                        |
| Throw a domain error                        | `throw new BusinessException(ErrorCode.SEAT_ALREADY_BOOKED)`         |
| Add audit fields to a JPA entity            | `extends AuditEntity`                                                |
| Route traffic to a service                  | Add a route in `api-gateway.yaml` in config-repo                     |
| Change DB URL without restarting            | Update `auth-service.yaml` in config-repo → call `/actuator/refresh` |
| Find which instance of a service is running | Check Eureka dashboard at `http://localhost:8761`                    |
