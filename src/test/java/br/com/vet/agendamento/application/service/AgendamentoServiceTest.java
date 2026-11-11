package br.com.vet.agendamento.application.service;

import br.com.vet.agendamento.application.dto.request.AgendamentoRequest;
import br.com.vet.agendamento.application.dto.response.AgendamentoResponse;
import br.com.vet.agendamento.application.exception.BusinessException;
import br.com.vet.agendamento.application.exception.ConflictException;
import br.com.vet.agendamento.application.gateway.AnimalInfo;
import br.com.vet.agendamento.application.gateway.IdentidadeGateway;
import br.com.vet.agendamento.application.gateway.TutorInfo;
import br.com.vet.agendamento.domain.enums.StatusAgendamento;
import br.com.vet.agendamento.domain.model.Agendamento;
import br.com.vet.agendamento.domain.model.Disponibilidade;
import br.com.vet.agendamento.domain.repository.AgendamentoRepository;
import br.com.vet.agendamento.infrastructure.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 5, 24);
    private static final Long TUTOR_ID = 15L;
    private static final Long ANIMAL_ID = 28L;
    private static final Long VETERINARIO_ID = 4L;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private DisponibilidadeService disponibilidadeService;

    @Mock
    private IdentidadeGateway identidadeGateway;

    @Mock
    private UsuarioAutenticado usuarioAutenticado;

    private AgendamentoService service;

    @BeforeEach
    void setUp() {
        service = new AgendamentoService(
                agendamentoRepository, disponibilidadeService, identidadeGateway, usuarioAutenticado);
    }

    @Test
    void tutorCriaAgendamentoParaOProprioAnimal() {
        autenticarTutorAtivo();
        prepararAgendamentoLivre(LocalTime.of(9, 0));

        AgendamentoResponse response = service.criar(request(null, LocalTime.of(9, 0)));

        assertThat(response.id()).isEqualTo(8L);
        assertThat(response.status()).isEqualTo("AGENDADO");
        assertThat(response.tutorId()).isEqualTo(TUTOR_ID);
        assertThat(response.animalId()).isEqualTo(ANIMAL_ID);
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(9, 30));
        verify(identidadeGateway).buscarTutorAutenticado();
        verify(identidadeGateway).buscarAnimalDoTutorAutenticado(ANIMAL_ID);
    }

    @Test
    void recepcionistaCriaAgendamentoParaTutorInformado() {
        when(usuarioAutenticado.isTutor()).thenReturn(false);
        when(usuarioAutenticado.isRecepcionista()).thenReturn(true);
        when(identidadeGateway.buscarTutor(TUTOR_ID)).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        when(identidadeGateway.buscarAnimalDoTutor(TUTOR_ID, ANIMAL_ID))
                .thenReturn(animal(ANIMAL_ID, TUTOR_ID));
        prepararAgendamentoLivre(LocalTime.of(9, 0));

        AgendamentoResponse response = service.criar(request(TUTOR_ID, LocalTime.of(9, 0)));

        assertThat(response.tutorId()).isEqualTo(TUTOR_ID);
        assertThat(response.animalId()).isEqualTo(ANIMAL_ID);
        verify(identidadeGateway).buscarTutor(TUTOR_ID);
        verify(identidadeGateway).buscarAnimalDoTutor(TUTOR_ID, ANIMAL_ID);
    }

    @Test
    void rejeitaAgendamentoQuandoTutorEstaDesativado() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado())
                .thenReturn(tutor(TUTOR_ID, "DESATIVADO"));

        assertThatThrownBy(() -> service.criar(request(null, LocalTime.of(9, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    void rejeitaTutorQueTentaAgendarParaOutroTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));

        assertThatThrownBy(() -> service.criar(request(99L, LocalTime.of(9, 0))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outro tutor");
    }

    @Test
    void rejeitaAnimalQueNaoPertenceAoTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        when(identidadeGateway.buscarAnimalDoTutorAutenticado(ANIMAL_ID))
                .thenReturn(animal(ANIMAL_ID, 99L));

        assertThatThrownBy(() -> service.criar(request(null, LocalTime.of(9, 0))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não pertence ao tutor");
    }

    @Test
    void rejeitaAgendamentoQuandoVeterinarioJaEstaOcupado() {
        autenticarTutorAtivo();
        when(disponibilidadeService.resolverDisponibilidade(VETERINARIO_ID, DATA, LocalTime.of(9, 0)))
                .thenReturn(disponibilidade());
        when(agendamentoRepository.existeConflito(
                eq(VETERINARIO_ID), eq(DATA), eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 30)),
                eq(StatusAgendamento.CANCELADO), eq(null)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criar(request(null, LocalTime.of(9, 0))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já possui um agendamento");
    }

    @Test
    void naoPermiteReagendarAgendamentoJaEmAtendimento() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(null, LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Somente agendamentos com status AGENDADO");
    }

    @Test
    void rejeitaTransicaoDeStatusForaDaSequencia() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.alterarStatus(8L, StatusAgendamento.CONCLUIDO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Transição de status inválida");
    }

    @Test
    void checkInRegistraHorarioEAvancaStatus() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgendamentoResponse response = service.alterarStatus(8L, StatusAgendamento.AGUARDANDO_ATENDIMENTO);

        assertThat(response.status()).isEqualTo("AGUARDANDO_ATENDIMENTO");
        assertThat(response.horarioCheckin()).isNotNull();
    }

    @Test
    void tutorReagendaProprioAgendamentoMantendoTutor() {
        autenticarTutorAtivo();
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        prepararReagendamentoLivre(LocalTime.of(10, 0), 8L);

        AgendamentoResponse response = service.atualizar(8L, request(null, LocalTime.of(10, 0)));

        assertThat(response.tutorId()).isEqualTo(TUTOR_ID);
        assertThat(response.animalId()).isEqualTo(ANIMAL_ID);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.horaFim()).isEqualTo(LocalTime.of(10, 30));
        assertThat(response.status()).isEqualTo("AGENDADO");
    }

    @Test
    void recepcionistaReagendaAgendamentoDeQualquerTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(false);
        when(identidadeGateway.buscarTutor(TUTOR_ID)).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        when(identidadeGateway.buscarAnimalDoTutor(TUTOR_ID, ANIMAL_ID))
                .thenReturn(animal(ANIMAL_ID, TUTOR_ID));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        prepararReagendamentoLivre(LocalTime.of(10, 0), 8L);

        AgendamentoResponse response = service.atualizar(8L, request(TUTOR_ID, LocalTime.of(10, 0)));

        assertThat(response.tutorId()).isEqualTo(TUTOR_ID);
        assertThat(response.horaInicio()).isEqualTo(LocalTime.of(10, 0));
        verify(identidadeGateway).buscarTutor(TUTOR_ID);
        verify(identidadeGateway).buscarAnimalDoTutor(TUTOR_ID, ANIMAL_ID);
    }

    @Test
    void rejeitaReagendamentoDeAgendamentoDeOutroTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        agendamento.setTutorId(99L);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(null, LocalTime.of(10, 0))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outro tutor");
    }

    @Test
    void rejeitaReagendamentoComAnimalDeOutroTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        when(identidadeGateway.buscarAnimalDoTutorAutenticado(ANIMAL_ID))
                .thenReturn(animal(ANIMAL_ID, 99L));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(null, LocalTime.of(10, 0))))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não pertence ao tutor");
    }

    @Test
    void rejeitaReagendamentoQuandoTutorEstaDesativado() {
        when(usuarioAutenticado.isTutor()).thenReturn(false);
        when(identidadeGateway.buscarTutor(TUTOR_ID)).thenReturn(tutor(TUTOR_ID, "DESATIVADO"));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(TUTOR_ID, LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    void rejeitaReagendamentoQueTentaAlterarTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(false);
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.atualizar(8L, request(99L, LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("alterar o tutor");
    }

    @Test
    void tutorCancelaProprioAgendamentoELiberaHorario() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(8L);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    void rejeitaCancelamentoDeAgendamentoDeOutroTutor() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGENDADO);
        agendamento.setTutorId(99L);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.cancelar(8L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outro tutor");
    }

    @Test
    void recepcionistaCancelaAgendamentoAguardandoAtendimento() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.AGUARDANDO_ATENDIMENTO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelar(8L);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void rejeitaCancelamentoDeAgendamentoConcluido() {
        Agendamento agendamento = agendamentoExistente(StatusAgendamento.CONCLUIDO);
        when(agendamentoRepository.findByIdAndDeletadoEmIsNull(8L))
                .thenReturn(Optional.of(agendamento));

        assertThatThrownBy(() -> service.cancelar(8L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("podem ser cancelados");
    }

    private void autenticarTutorAtivo() {
        when(usuarioAutenticado.isTutor()).thenReturn(true);
        when(identidadeGateway.buscarTutorAutenticado()).thenReturn(tutor(TUTOR_ID, "ATIVO"));
        when(identidadeGateway.buscarAnimalDoTutorAutenticado(ANIMAL_ID))
                .thenReturn(animal(ANIMAL_ID, TUTOR_ID));
    }

    private void prepararAgendamentoLivre(LocalTime horaInicio) {
        when(disponibilidadeService.resolverDisponibilidade(VETERINARIO_ID, DATA, horaInicio))
                .thenReturn(disponibilidade());
        when(agendamentoRepository.existeConflito(
                eq(VETERINARIO_ID), eq(DATA), eq(horaInicio), eq(horaInicio.plusMinutes(30)),
                eq(StatusAgendamento.CANCELADO), eq(null)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> {
                    Agendamento agendamento = invocation.getArgument(0);
                    agendamento.setId(8L);
                    return agendamento;
                });
    }

    private void prepararReagendamentoLivre(LocalTime horaInicio, Long idIgnorado) {
        when(disponibilidadeService.resolverDisponibilidade(VETERINARIO_ID, DATA, horaInicio))
                .thenReturn(disponibilidade());
        when(agendamentoRepository.existeConflito(
                eq(VETERINARIO_ID), eq(DATA), eq(horaInicio), eq(horaInicio.plusMinutes(30)),
                eq(StatusAgendamento.CANCELADO), eq(idIgnorado)))
                .thenReturn(false);
        when(agendamentoRepository.save(any(Agendamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AgendamentoRequest request(Long tutorId, LocalTime horaInicio) {
        return new AgendamentoRequest(tutorId, ANIMAL_ID, VETERINARIO_ID, DATA, horaInicio);
    }

    private TutorInfo tutor(Long id, String status) {
        return new TutorInfo(id, 100L + id, "Tutor " + id, status);
    }

    private AnimalInfo animal(Long id, Long tutorId) {
        return new AnimalInfo(id, tutorId, "Animal " + id);
    }

    private Disponibilidade disponibilidade() {
        Disponibilidade disponibilidade = new Disponibilidade();
        disponibilidade.setId(1L);
        disponibilidade.setVeterinarioId(VETERINARIO_ID);
        disponibilidade.setHoraInicio(LocalTime.of(8, 0));
        disponibilidade.setHoraFim(LocalTime.of(12, 0));
        disponibilidade.setDuracaoConsulta(30);
        return disponibilidade;
    }

    private Agendamento agendamentoExistente(StatusAgendamento status) {
        Agendamento agendamento = new Agendamento();
        agendamento.setId(8L);
        agendamento.setTutorId(TUTOR_ID);
        agendamento.setAnimalId(ANIMAL_ID);
        agendamento.setVeterinarioId(VETERINARIO_ID);
        agendamento.setData(DATA);
        agendamento.setHoraInicio(LocalTime.of(9, 0));
        agendamento.setHoraFim(LocalTime.of(9, 30));
        agendamento.setStatus(status);
        return agendamento;
    }
}
