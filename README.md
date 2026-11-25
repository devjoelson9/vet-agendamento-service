# Serviço de Agendamento — Sistema de Gestão Veterinária

Microsserviço responsável pela agenda dos veterinários: disponibilidades, agendamentos, reagendamento, cancelamento e check-in na recepção.

## Tecnologias

- Java 21 e Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA, Spring Security (JWT) e Bean Validation
- PostgreSQL com migrações Flyway
- RabbitMQ para consumo dos eventos de atendimento
- Spring Boot Actuator e springdoc-openapi (Swagger UI)

## Rotas principais

| Recurso | Prefixo |
|---|---|
| Agendamentos (horários disponíveis, fila de espera, check-in, status) | `/api/v1/agendamentos` |
| Disponibilidades dos veterinários | `/api/v1/disponibilidades` |
| Health check | `/api/actuator/health` |

As rotas de negócio exigem o header `Authorization: Bearer <token>`; o health check é público (usado pelo frontend).

Documentação interativa: `http://localhost:8084/swagger-ui.html`.

## Integrações

- **Identidade** (`IDENTIDADE_BASE_URL`): valida tutor, animal e veterinário nas operações de agendamento, repassando o token recebido.
- **RabbitMQ**: consome o exchange `vet.events`. Ao receber `atendimento.realizado` o agendamento passa para `ATENDIMENTO_REALIZADO`; `atendimento.cancelado` o devolve para `AGUARDANDO_ATENDIMENTO`. O evento `atendimento.iniciado` não altera o status, que permanece `AGUARDANDO_ATENDIMENTO` até a finalização clínica.

## Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET` | Mesmo segredo usado pelos demais serviços |
| `IDENTIDADE_BASE_URL` | Ex.: `http://servico-identidade:8080` |
| `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` | Broker de mensageria |

## Como executar

Ambiente completo (recomendado), a partir de `vet-infrastructure`:

```bash
docker compose up -d --build servico-agendamento
```

Execução local, com um PostgreSQL disponível:

```bash
./mvnw spring-boot:run
```

Testes:

```bash
./mvnw test
```

## Estrutura

```text
src/main/java/br/com/vet/agendamento/
├── application/     # serviços de caso de uso, gateways, mappers e DTOs
├── domain/          # entidades, enums e repositórios
├── infrastructure/  # integrações HTTP/AMQP, segurança e configuração
└── web/             # controllers e tratamento de exceções
```
