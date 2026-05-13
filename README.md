# Czanix Boilerplate — API Java

> Spring Boot com arquitetura hexagonal. O Java que funciona em produção sem precisar de 47 annotations para uma rota simples.

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16?style=flat&logo=postgresql&logoColor=white)](https://postgresql.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Tech Reference](https://img.shields.io/badge/Czanix-Tech%20Reference-gold)](https://czanix.com/pt/stack)

---

## Filosofia

Java é verboso por natureza. Isso não precisa ser um problema se a verbosidade traz clareza.

**Regras deste boilerplate:**
1. Camada de dominio nao conhece Spring, JPA, nem HTTP
2. Caso de uso recebe input e retorna `Result<T>` — sem exceção para fluxo de negócio
3. Controller é fino: valida, chama use case, retorna
4. Repository é interface no dominio, implementação na infraestrutura

**O que não tem aqui:** Lombok desnecessário, annotations sobre annotations, `@Autowired` em field (constructor injection apenas), DTO genérico que serve pra tudo e não serve pra nada.

---

## Estrutura

```
src/main/java/com/czanix/api/
├── domain/                          # Zero dependências do Spring
│   ├── entities/
│   │   └── Order.java               # POJO puro com regras de negócio
│   ├── repositories/
│   │   └── OrderRepository.java     # Interface — contrato
│   └── Result.java                  # Sealed class: Success | Failure
│
├── application/                     # Casos de uso
│   ├── usecases/
│   │   ├── CreateOrderUseCase.java
│   │   └── CancelOrderUseCase.java
│   └── dtos/
│       ├── CreateOrderInput.java    # Record (Java 21)
│       └── OrderOutput.java
│
├── infrastructure/                  # Mundo externo
│   ├── persistence/
│   │   ├── JpaOrderRepository.java  # Implementação concreta
│   │   └── entities/
│   │       └── OrderJpaEntity.java  # Entity JPA separada do domínio
│   ├── config/
│   │   ├── SecurityConfig.java      # OWASP headers, CORS, CSP
│   │   └── CacheConfig.java         # Redis config
│   └── migrations/                  # Flyway SQL versionado
│
└── presentation/                    # Controllers
    ├── controllers/
    │   └── OrderController.java     # @RestController fino
    ├── advice/
    │   └── GlobalExceptionHandler.java
    └── validators/
        └── OrderValidator.java
```

### Por que separar `OrderJpaEntity` do `Order` do domínio?

Porque JPA polui a entidade com `@Column`, `@Table`, `@ManyToOne`. O domínio não precisa saber que existe banco de dados. Mapper manual entre os dois é o preço — e vale cada linha.

---

## Início rápido

```bash
# 1. Clone
git clone https://github.com/czanix/boilerplate-api-java.git meu-projeto
cd meu-projeto

# 2. Build
./mvnw clean install

# 3. Ambiente
cp .env.example .env
# Edite com suas variáveis

# 4. Banco + Cache
docker compose up -d

# 5. Migrations (Flyway roda automaticamente no boot)
./mvnw spring-boot:run
```

---

## Result Pattern — sem exceção para fluxo de negócio

```java
// Result.java — sealed para segurança de tipo
public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error) implements Result<T> {}

    static <T> Result<T> ok(T value) { return new Success<>(value); }
    static <T> Result<T> fail(String error) { return new Failure<>(error); }
}

// Uso no use case
public Result<OrderOutput> execute(CreateOrderInput input) {
    if (input.items().isEmpty()) {
        return Result.fail("Pedido sem itens");
    }

    var order = Order.create(input.customerId(), input.items());
    repository.save(order);

    return Result.ok(OrderOutput.from(order));
}

// Controller — o chamador trata explicitamente
@PostMapping("/orders")
public ResponseEntity<?> create(@Valid @RequestBody CreateOrderInput input) {
    var result = createOrderUseCase.execute(input);

    return switch (result) {
        case Result.Success<OrderOutput> s -> ResponseEntity
            .status(201).body(s.value());
        case Result.Failure<OrderOutput> f -> ResponseEntity
            .status(422).body(Map.of("error", f.error()));
    };
}
```

**Por que não `throw`?** "Email já cadastrado" não é exceção. É fluxo de negócio. Exceção é para o inesperado — `OutOfMemoryError`, `ConnectionRefused`. [Mais detalhes →](https://czanix.com/pt/stack/tradeoffs)

---

## Schema SQL

```sql
CREATE TABLE orders (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id   UUID NOT NULL DEFAULT gen_random_uuid(),
    deleted_at  TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_orders_public_id UNIQUE (public_id)
);

CREATE INDEX ix_orders_customer_active
    ON orders (customer_id, created_at DESC)
    WHERE deleted_at IS NULL;
```

**BIGINT como PK, UUID para exposição externa.** O ID sequencial garante performance de B-Tree. O UUID impede enumeração (OWASP A01). [ADR completo →](https://czanix.com/pt/stack/dados)

---

## Segurança (OWASP embutido)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            )
            .csrf(AbstractHttpConfigurer::disable) // APIs stateless
            .build();
    }
}
```

---

## Testes

```bash
./mvnw test                    # Unit tests — sem banco
./mvnw verify                  # Integration tests com Testcontainers
./mvnw jacoco:report           # Coverage report
```

**Meta:** 80%+ em `domain/` e `application/`. Infraestrutura é testada com Testcontainers.

---

## Architecture Decision Records (ADRs)

Decisões arquiteturais documentadas com contexto, motivo e trade-offs:

- [ADR-001: INT/BIGINT PK + UUID público](docs/adrs/001-bigint-pk-uuid-public.md)
- [ADR-002: Result Pattern vs Exceptions](docs/adrs/002-result-pattern-over-exceptions.md)
- [ADR-003: Clean Architecture com limites pragmáticos](docs/adrs/003-clean-architecture-boundaries.md)
- [ADR-004: Princípios de Modelagem de Dados](docs/adrs/004-database-design-principles.md)
- [ADR-005: Partitioning para Tabelas de Alto Volume](docs/adrs/005-table-partitioning.md)
- [ADR-006: Connection Pooling e Pool Sizing](docs/adrs/006-connection-pooling.md)
- [ADR-007: VACUUM, Autovacuum e Bloat Prevention](docs/adrs/007-vacuum-autovacuum.md)
- [ADR-008: Read Replicas e Separação de Leitura/Escrita](docs/adrs/008-read-replicas.md)
- [ADR-009: Observabilidade e Testes de Carga](docs/adrs/009-observability-load-testing.md)

---

## Referência técnica

- [Guia de Backend & Arquitetura](https://czanix.com/pt/stack/backend)
- [Guia de Database](https://czanix.com/pt/stack/dados)
- [Catálogo de Trade-offs](https://czanix.com/pt/stack/tradeoffs)
- [DevOps & CI/CD](https://czanix.com/pt/stack/devops)

---

## Licença

MIT — use, adapte, melhore. Se ajudou, [deixa uma estrela](https://github.com/czanix/boilerplate-api-java) ⭐

---

<div align="center">
<sub>Desenvolvido e mantido por <a href="https://czanix.com">Cesar Zanis</a> — Czanix</sub>
</div>
