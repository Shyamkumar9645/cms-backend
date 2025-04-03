version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:14-alpine
    container_name: cms-postgres
    environment:
      POSTGRES_DB: cms-db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 1234
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - cms-network

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: cms-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD:-redispassword}
    restart: unless-stopped
    networks:
      - cms-network

  # Redis Commander (Optional Redis UI)
  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: cms-redis-commander
    environment:
      - REDIS_HOSTS=local:redis:6379:0:${REDIS_PASSWORD:-redispassword}
    ports:
      - "8081:8081"
    depends_on:
      - redis
    restart: unless-stopped
    networks:
      - cms-network

  # Spring Boot Application (uncomment to run with docker-compose)
  #app:
  #  build: .
  #  container_name: cms-app
  #  depends_on:
  #    - postgres
  #    - redis
  #  environment:
  #    - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cms-db
  #    - SPRING_DATASOURCE_USERNAME=postgres
  #    - SPRING_DATASOURCE_PASSWORD=1234
  #    - SPRING_REDIS_HOST=redis
  #    - SPRING_REDIS_PORT=6379
  #    - SPRING_REDIS_PASSWORD=${REDIS_PASSWORD:-redispassword}
  #  ports:
  #    - "8080:8080"
  #  networks:
  #    - cms-network

networks:
  cms-network:
    driver: bridge

volumes:
  postgres-data:
  redis-data: