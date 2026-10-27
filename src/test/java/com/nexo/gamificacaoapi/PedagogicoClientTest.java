package com.nexo.gamificacaoapi;

import com.nexo.gamificacaoapi.service.PedagogicoClient;
import com.nexo.gamificacaoapi.service.PedagogicoClient.ContextoAluno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O que a gamificação faz com a resposta (ou a falta dela) do pedagogico-api. */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:teste-gamificacao-ped;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class PedagogicoClientTest extends TesteApiBase {

    @MockitoBean PedagogicoClient pedagogico;

    @Test
    @DisplayName("esconde desafios de matérias fora da etapa do aluno e mostra a turma no dashboard")
    void filtraPorEtapa() throws Exception {
        when(pedagogico.contextoDoAluno())
                .thenReturn(new ContextoAluno("2º Ano - Ensino Médio", "MEDIO", List.of("Inglês", "biologia")));
        when(pedagogico.contextoDoAlunoOuVazio())
                .thenReturn(new ContextoAluno("2º Ano - Ensino Médio", "MEDIO", List.of()));

        mvc.perform(get("/api/aluno/desafios").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                // 6 semeados − 1 de Inglês − 2 de Biologia
                .andExpect(jsonPath("$.desafios.length()").value(3))
                .andExpect(jsonPath("$.stats.total").value(3));
        mvc.perform(get("/api/aluno/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(jsonPath("$.turmaNome").value("2º Ano - Ensino Médio"));
    }

    @Test
    @DisplayName("pedagogico-api fora do ar: a lista de desafios é 503, mas o dashboard abre sem a turma")
    void pedagogicoForaDoAr() throws Exception {
        when(pedagogico.contextoDoAluno())
                .thenThrow(com.nexo.gamificacaoapi.api.ApiException.servicoIndisponivel("fora"));
        when(pedagogico.contextoDoAlunoOuVazio()).thenReturn(ContextoAluno.semRestricao());

        mvc.perform(get("/api/aluno/desafios").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/aluno/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turmaNome").doesNotExist());
    }
}
