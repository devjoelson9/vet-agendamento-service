package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.BloqueioAgendaRequest;
import br.com.vet.agendamento.application.dto.response.BloqueioAgendaResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.BloqueioAgenda;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.domain.repository.BloqueioAgendaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueioAgendaService {

    private final BloqueioAgendaRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public BloqueioAgendaResponse criar(BloqueioAgendaRequest request) {
        if (!request.fimEm().isAfter(request.inicioEm())) {
            throw new BusinessException("O fim do bloqueio deve ser posterior ao início");
        }
        if (agendamentoRepository.existeConflito(
                request.veterinarioId(), request.inicioEm(), request.fimEm(),
                List.of(
                        StatusAgendamento.AGUARDANDO_CONFIRMACAO.name(),
                        StatusAgendamento.CONFIRMADO.name(),
                        StatusAgendamento.CHECK_IN.name(),
                        StatusAgendamento.EM_ATENDIMENTO.name()
                ), null)) {
            throw new ConflictException("Já existe um agendamento no período informado");
        }
        if (bloqueioRepository.existeConflito(
                request.veterinarioId(), request.inicioEm(), request.fimEm())) {
            throw new ConflictException("Já existe um bloqueio no período informado");
        }

        BloqueioAgenda bloqueio = new BloqueioAgenda();
        bloqueio.setVeterinarioId(request.veterinarioId());
        bloqueio.setMotivo(request.motivo());
        bloqueio.setInicioEm(request.inicioEm());
        bloqueio.setFimEm(request.fimEm());
        return BloqueioAgendaResponse.from(bloqueioRepository.save(bloqueio));
    }

    @Transactional(readOnly = true)
    public List<BloqueioAgendaResponse> listar(LocalDateTime inicio, LocalDateTime fim, Long veterinarioId) {
        if (inicio == null || fim == null || !fim.isAfter(inicio)) {
            throw new BusinessException("O período deve conter início e fim válidos, com fim posterior ao início");
        }
        return bloqueioRepository.buscarNoPeriodo(inicio, fim, veterinarioId)
                .stream()
                .map(BloqueioAgendaResponse::from)
                .toList();
    }

    @Transactional
    public void excluir(Long id) {
        if (!bloqueioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bloqueio de agenda não encontrado: " + id);
        }
        bloqueioRepository.deleteById(id);
    }
}
