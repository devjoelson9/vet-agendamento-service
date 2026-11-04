# vet-agendamento-service

Microsserviço de agendamento veterinário, alinhado ao padrão Spring Boot usado pelos serviços MaisVet. Armazena referências aos IDs de tutores, animais e veterinários (sem replicar os cadastros dos outros serviços), além de consultas, procedimentos e bloqueios de agenda.

## Requisitos

- Java 21
- PostgreSQL 16+
- Token JWT assinado com o mesmo segredo usado pelos outros serviços

## Execução local

1. Copie `.env.example` para `.env` e configure `JWT_SECRET` com a chave compartilhada com os outros serviços.
2. Inicie o PostgreSQL, por exemplo, com `docker compose up -d agendamento-db`.
3. Execute `.\mvnw.cmd spring-boot:run`.

O serviço atende por padrão em `http://localhost:8080`. Para executar API local fora do Docker, configure as variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`. O Flyway cria o schema automaticamente.

Também é possível iniciar serviço e banco juntos com `docker compose up --build`; a API ficará na porta `8084` por padrão.

## API

Todas as rotas da API exigem `Authorization: Bearer <access-token>`. O health check `GET /actuator/health` é público. Datas são ISO-8601 sem fuso, por exemplo `2025-05-24T14:45:00`.

### Agendamentos

- `POST /api/v1/agendamentos` — cria como `AGUARDANDO_CONFIRMACAO`.
- `GET /api/v1/agendamentos?inicio=...&fim=...` — lista eventos no intervalo; aceita filtros `veterinarioId`, `animalId` e `status`.
- `GET /api/v1/agendamentos/{id}` — consulta os detalhes.
- `PUT /api/v1/agendamentos/{id}` — atualiza os dados do agendamento.
- `PATCH /api/v1/agendamentos/{id}/status` — transiciona o status, por exemplo `{"status":"CONFIRMADO"}` ou `{"status":"CHECK_IN"}`.
- `DELETE /api/v1/agendamentos/{id}` — cancela sem remover o histórico.

Exemplo de criação:

```json
{
  "tutorId": 15,
  "animalId": 28,
  "veterinarioId": 4,
  "tipo": "CONSULTA",
  "servicoNome": "Consulta de Retorno / Acompanhamento",
  "inicioEm": "2025-05-24T14:45:00",
  "duracaoMinutos": 30,
  "observacoes": "Verificação de pós-operatório e retirada de pontos"
}
```

O fim do evento é calculado a partir do início e da duração. Agendamentos ativos do mesmo veterinário não podem se sobrepor; horários bloqueados também impedem novas reservas. Cancelados, concluídos e faltas não ocupam o horário.

### Bloqueios de agenda

- `POST /api/v1/bloqueios-agenda` — cria um bloqueio. `veterinarioId` nulo representa bloqueio geral, como almoço ou higienização.
- `GET /api/v1/bloqueios-agenda?inicio=...&fim=...` — lista bloqueios no período; aceita o filtro opcional `veterinarioId`.
- `DELETE /api/v1/bloqueios-agenda/{id}` — remove o bloqueio.
