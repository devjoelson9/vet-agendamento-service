CREATE TABLE disponibilidade (
    id BIGSERIAL PRIMARY KEY,
    veterinario_id BIGINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    duracao_consulta INTEGER NOT NULL CHECK (duracao_consulta > 0), 
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT ck_disponibilidade_horario CHECK (hora_fim > hora_inicio)
);

CREATE UNIQUE INDEX idx_disponibilidade_vet_dia_inicio_ativo
    ON disponibilidade (veterinario_id, dia_semana, hora_inicio)
    WHERE deletado_em IS NULL;

CREATE TABLE agendamento (
    id BIGSERIAL PRIMARY KEY,
    tutor_id BIGINT NOT NULL,
    animal_id BIGINT NOT NULL,
    veterinario_id BIGINT NOT NULL,
    data DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    horario_checkin TIMESTAMP,
    status VARCHAR(30) NOT NULL, 
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    deletado_em TIMESTAMP,
    CONSTRAINT ck_agendamento_horario CHECK (hora_fim > hora_inicio)
);

CREATE UNIQUE INDEX idx_agendamento_vet_horario_ativo
    ON agendamento (veterinario_id, data, hora_inicio)
    WHERE deletado_em IS NULL AND status <> 'CANCELADO';

CREATE INDEX idx_agendamento_animal ON agendamento (animal_id) WHERE deletado_em IS NULL;
CREATE INDEX idx_agendamento_tutor ON agendamento (tutor_id) WHERE deletado_em IS NULL;
