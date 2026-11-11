package br.com.vet.agendamento.infrastructure.integration;

import br.com.vet.agendamento.application.exception.IntegrationException;
import br.com.vet.agendamento.application.exception.ResourceNotFoundException;
import br.com.vet.agendamento.application.gateway.AnimalInfo;
import br.com.vet.agendamento.application.gateway.IdentidadeGateway;
import br.com.vet.agendamento.application.gateway.TutorInfo;
import br.com.vet.agendamento.infrastructure.security.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdentidadeClient implements IdentidadeGateway {

    private final RestClient restClient;
    private final UsuarioAutenticado usuarioAutenticado;

    public IdentidadeClient(
            RestClient.Builder restClientBuilder,
            @Value("${identidade.base-url}") String baseUrl,
            UsuarioAutenticado usuarioAutenticado
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    public TutorInfo buscarTutorAutenticado() {
        return get("/api/v1/tutores/me", TutorInfo.class);
    }

    @Override
    public TutorInfo buscarTutor(Long tutorId) {
        return get("/api/v1/recepcionista/tutores/{tutorId}", TutorInfo.class, tutorId);
    }

    @Override
    public AnimalInfo buscarAnimalDoTutorAutenticado(Long animalId) {
        return get("/api/v1/tutores/me/animais/{animalId}", AnimalInfo.class, animalId);
    }

    @Override
    public AnimalInfo buscarAnimalDoTutor(Long tutorId, Long animalId) {
        return get("/api/v1/recepcionista/tutores/{tutorId}/animais/{animalId}",
                AnimalInfo.class, tutorId, animalId);
    }

    private <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(uri, uriVariables)
                    .headers(headers -> {
                        String authorization = usuarioAutenticado.authorizationHeader();
                        if (authorization != null) {
                            headers.set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    })
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404) {
                throw new ResourceNotFoundException("Registro não encontrado no serviço de identidade");
            }
            if (status == 401 || status == 403) {
                throw new AccessDeniedException("Acesso negado pelo serviço de identidade");
            }
            throw new IntegrationException("Falha ao consultar o serviço de identidade", exception);
        } catch (ResourceAccessException exception) {
            throw new IntegrationException("Serviço de identidade indisponível", exception);
        }
    }
}
