## How to run
1. Start Docker Desktop
2. `docker compose up -d` and wait for healthy
3. `mvn -q install` from the repo root
4. `cd order-service && mvn spring-boot:run`
5. Check `localhost:8080/actuator/health`

## Ports
Postgres 5432, Redis 6379, Kafka 9092, Prometheus 9090, Grafana 3000, app 8080
