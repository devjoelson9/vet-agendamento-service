CREATE TABLE agendamento (
    id BIGSERIAL PRIMARY KEY,
    tutor_id BIGINT NOT NULL CHECK (tutor_id > 0),
    animal_id BIGINT NOT NULL CHECK (animal_id > 0),
    veterinario_id BIGINT NOT NULL CHECK (veterinario_id > 0),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CONSULTA', 'PROCEDIMENTO')),
    servico_nome VARCHAR(120) NOT NULL,
    inicio_em TIMESTAMP NOT NULL,
    duracao_minutos INTEGER NOT NULL CHECK (duracao_minutos BETWEEN 5 AND 480),
    status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_CONFIRMACAO'
        CHECK (status IN (
            'AGUARDANDO_CONFIRMACAO', 'CONFIRMADO', 'CHECK_IN',
            'EM_ATENDIMENTO', 'CONCLUIDO', 'CANCELADO', 'FALTOU'
        )),
    observacoes TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP
);

CREATE INDEX idx_agendamento_inicio_ativo
    ON agendamento (inicio_em) WHERE deletado_em IS NULL;
CREATE INDEX idx_agendamento_veterinario_periodo
    ON agendamento (veterinario_id, inicio_em) WHERE deletado_em IS NULL;
CREATE INDEX idx_agendamento_animal_periodo
    ON agendamento (animal_id, inicio_em) WHERE deletado_em IS NULL;

CREATE TABLE bloqueio_agenda (
    id BIGSERIAL PRIMARY KEY,
    veterinario_id BIGINT CHECK (veterinario_id IS NULL OR veterinario_id > 0),
    motivo VARCHAR(160) NOT NULL,
    inicio_em TIMESTAMP NOT NULL,
    fim_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_bloqueio_agenda_periodo CHECK (fim_em > inicio_em)
);

CREATE INDEX idx_bloqueio_agenda_periodo
    ON bloqueio_agenda (inicio_em, fim_em);
CREATE INDEX idx_bloqueio_agenda_veterinario
    ON bloqueio_agenda (veterinario_id, inicio_em);
