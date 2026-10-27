package com.nexo.gamificacaoapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexo.gamificacaoapi.domain.Desafio;
import com.nexo.gamificacaoapi.repository.DesafioRepository;
import com.nexo.gamificacaoapi.repository.JogadorRepository;
import com.nexo.gamificacaoapi.repository.QuizPerguntaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Desafios, quiz, dashboard/ranking, privacidade, sincronização e catálogo do admin. */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:teste-gamificacao;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class GamificacaoTest extends TesteApiBase {

    @Autowired DesafioRepository desafios;
    @Autowired JogadorRepository jogadores;
    @Autowired QuizPerguntaRepository perguntas;

    private long desafioId(String titulo) {
        return desafios.findAll().stream().filter(d -> d.getTitulo().equals(titulo))
                .findFirst().orElseThrow().getId();
    }

    private JsonNode corpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private String respostas(long desafioId, boolean todasCertas) {
        List<String> itens = new ArrayList<>();
        var lista = perguntas.findByDesafioIdOrderById(desafioId);
        for (int i = 0; i < lista.size(); i++) {
            var p = lista.get(i);
            int escolha = todasCertas || i > 0 ? p.getRespostaCorreta() : (p.getRespostaCorreta() + 1) % 4;
            itens.add("{\"perguntaId\":" + p.getId() + ",\"alternativaEscolhida\":" + escolha + "}");
        }
        return "{\"respostas\":[" + String.join(",", itens) + "]}";
    }

    // ── Acesso ──

    @Test
    @DisplayName("sem token é 401; professor não entra nos desafios do aluno; aluno não entra no admin")
    void acesso() throws Exception {
        mvc.perform(get("/api/aluno/desafios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aluno/desafios").header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/catalogo/desafios").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isForbidden());
    }

    // ── Desafios ──

    @Test
    @DisplayName("lista os desafios semeados com o progresso do aluno e as estatísticas")
    void lista() throws Exception {
        MvcResult r = mvc.perform(get("/api/aluno/desafios").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk()).andReturn();
        JsonNode c = corpo(r);
        assertThat(c.get("desafios")).hasSize(6);
        assertThat(c.get("stats").get("total").asInt()).isEqualTo(6);
        assertThat(c.get("stats").get("concluidos").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(c.get("desafios").get(0).get("titulo").asText()).isEqualTo("Quiz: Fotossíntese");
        assertThat(c.get("desafios").get(0).get("status").asText()).isEqualTo("CONCLUIDO");
    }

    @Test
    @DisplayName("concluir credita o XP uma única vez")
    void concluirCreditaUmaVez() throws Exception {
        long id = desafioId("Present Perfect");
        int antes = jogadores.findById(1L).orElseThrow().getXpTotal();

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/aluno/desafios/" + id + "/concluir")
                            .header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONCLUIDO"));
        }
        assertThat(jogadores.findById(1L).orElseThrow().getXpTotal()).isEqualTo(antes + 100);
    }

    @Test
    @DisplayName("iniciar põe em PROGRESSO; desafio inexistente é 404")
    void iniciar() throws Exception {
        long id = desafioId("Revolução Industrial");
        mvc.perform(post("/api/aluno/desafios/" + id + "/iniciar").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROGRESSO"))
                .andExpect(jsonPath("$.progresso").value(10));
        mvc.perform(get("/api/aluno/desafios/99999").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isNotFound());
    }

    // ── Quiz ──

    @Test
    @DisplayName("quiz reprovado não credita XP, não revela placar nem gabarito; aprovado credita uma vez")
    void quiz() throws Exception {
        long id = desafioId("Ecossistemas e Biomas");
        int antes = jogadores.findById(1L).orElseThrow().getXpTotal();

        // abrir o quiz esconde o gabarito
        mvc.perform(get("/api/aluno/desafios/" + id + "/quiz").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].respostaCorreta").doesNotExist());

        mvc.perform(post("/api/aluno/desafios/" + id + "/quiz/finalizar").contentType(MediaType.APPLICATION_JSON)
                        .content(respostas(id, false)).header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprovado").value(false))
                .andExpect(jsonPath("$.acertos").doesNotExist())
                .andExpect(jsonPath("$.xpGanho").value(0))
                .andExpect(jsonPath("$.status").value("PROGRESSO"))
                .andExpect(jsonPath("$.tentativas").value(1));
        assertThat(jogadores.findById(1L).orElseThrow().getXpTotal()).isEqualTo(antes);

        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/aluno/desafios/" + id + "/quiz/finalizar").contentType(MediaType.APPLICATION_JSON)
                            .content(respostas(id, true)).header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aprovado").value(true))
                    .andExpect(jsonPath("$.status").value("CONCLUIDO"));
        }
        assertThat(jogadores.findById(1L).orElseThrow().getXpTotal()).isEqualTo(antes + 120);

        // concluído: o gabarito aparece
        mvc.perform(get("/api/aluno/desafios/" + id + "/quiz").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(jsonPath("$[0].respostaCorreta").exists());
    }

    // ── Dashboard e ranking ──

    @Test
    @DisplayName("dashboard traz nível, posição e ranking com o próprio aluno; turma vazia sem pedagogico-api")
    void dashboard() throws Exception {
        MvcResult r = mvc.perform(get("/api/aluno/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isOk()).andReturn();
        JsonNode c = corpo(r);
        assertThat(c.get("nome").asText()).isEqualTo("Gabriel Mendes");
        assertThat(c.get("totalAlunos").asInt()).isEqualTo((int) jogadores.count());
        assertThat(c.get("atividades")).hasSize(4);
        assertThat(c.get("foraDoRanking").asBoolean()).isFalse();
        assertThat(c.get("turmaNome").isNull()).isTrue();
        assertThat(c.get("nivel").asInt()).isEqualTo(1 + c.get("xpTotal").asInt() / 400);
        boolean euNoRanking = false;
        for (JsonNode i : c.get("ranking")) euNoRanking |= i.get("isMe").asBoolean();
        assertThat(euNoRanking).isTrue();
    }

    @Test
    @DisplayName("quem desliga o ranking sai dele e da contagem; a foto de quem é privado não vaza")
    void privacidade() throws Exception {
        jogadores.findById(2L).ifPresent(j -> { j.setFoto("/fotos/ana.png"); j.setXpTotal(99999); jogadores.save(j); });
        try {
            mvc.perform(patch("/api/sync/usuarios/3/privacidade").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"exibirNoRanking\":false}").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(status().isNoContent());
            mvc.perform(get("/api/aluno/dashboard").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(jsonPath("$.foraDoRanking").value(true))
                    .andExpect(jsonPath("$.posicao").value(0))
                    .andExpect(jsonPath("$.totalAlunos").value((int) jogadores.count() - 1));
        } finally {
            jogadores.findById(1L).ifPresent(j -> { j.setExibirNoRanking(true); jogadores.save(j); });
        }

        // Ana (líder) com perfil privado: aparece no ranking, mas sem a foto.
        jogadores.findById(2L).ifPresent(j -> { j.setPerfilPublico(false); jogadores.save(j); });
        try {
            JsonNode topo = corpo(mvc.perform(get("/api/aluno/dashboard")
                    .header(HttpHeaders.AUTHORIZATION, bearer("aluno"))).andReturn()).get("ranking").get(0);
            assertThat(topo.get("nome").asText()).isEqualTo("Ana Beatriz Souza");
            assertThat(topo.get("foto").isNull()).isTrue();
        } finally {
            jogadores.findById(2L).ifPresent(j -> { j.setPerfilPublico(true); j.setFoto(null); jogadores.save(j); });
        }
    }

    // ── Sincronização ──

    @Test
    @DisplayName("sync de aluno é upsert pelo id, mantém o XP e só a gestão chama; perfil só pelo dono ou gestão")
    void sincronizacao() throws Exception {
        String corpo = "{\"usuarioId\":900,\"nome\":\"Aluno Novo\",\"foto\":null}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/sync/alunos/500").contentType(MediaType.APPLICATION_JSON).content(corpo)
                            .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                    .andExpect(status().isNoContent());
        }
        long total = jogadores.count();
        var novo = jogadores.findById(500L).orElseThrow();
        assertThat(novo.getUsuarioId()).isEqualTo(900L);
        assertThat(novo.isExibirNoRanking()).isTrue();

        novo.setXpTotal(777);
        jogadores.save(novo);
        mvc.perform(put("/api/sync/alunos/500").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":900,\"nome\":\"Aluno Renomeado\",\"foto\":null}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin")))
                .andExpect(status().isNoContent());
        assertThat(jogadores.count()).isEqualTo(total);
        assertThat(jogadores.findById(500L).orElseThrow().getXpTotal()).isEqualTo(777);

        mvc.perform(put("/api/sync/alunos/501").contentType(MediaType.APPLICATION_JSON).content(corpo)
                        .header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/sync/alunos/501").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":901,\"nome\":\"\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("diretor")))
                .andExpect(status().isBadRequest());

        // o aluno (usuário 3) muda o próprio perfil; não muda o de outro
        mvc.perform(patch("/api/sync/usuarios/3/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Gabriel M.\",\"foto\":\"/f.png\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isNoContent());
        assertThat(jogadores.findById(1L).orElseThrow().getNome()).isEqualTo("Gabriel M.");
        mvc.perform(patch("/api/sync/usuarios/900/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Invasor\"}").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                .andExpect(status().isForbidden());
        // conta sem jogador (professor) é ignorada sem erro
        mvc.perform(patch("/api/sync/usuarios/2/perfil").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Prof\"}").header(HttpHeaders.AUTHORIZATION, bearer("professor")))
                .andExpect(status().isNoContent());

        jogadores.findById(1L).ifPresent(j -> { j.setNome("Gabriel Mendes"); j.setFoto(null); jogadores.save(j); });
    }

    // ── Catálogo do admin ──

    @Test
    @DisplayName("admin despublica e republica; desafio fora do ar some do aluno e responde 404")
    void catalogoAdmin() throws Exception {
        long id = desafioId("Derivadas e Integrais");
        mvc.perform(get("/api/admin/catalogo/desafios").header(HttpHeaders.AUTHORIZATION, bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
        try {
            mvc.perform(patch("/api/admin/catalogo/desafios/" + id + "/publicado")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"publicado\":false}")
                            .header(HttpHeaders.AUTHORIZATION, bearer("admin")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.publicado").value(false));
            mvc.perform(get("/api/aluno/desafios").header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(jsonPath("$.desafios.length()").value(5));
            mvc.perform(get("/api/aluno/desafios/" + id).header(HttpHeaders.AUTHORIZATION, bearer("aluno")))
                    .andExpect(status().isNotFound());
            mvc.perform(patch("/api/admin/catalogo/desafios/" + id + "/publicado")
                            .contentType(MediaType.APPLICATION_JSON).content("{}")
                            .header(HttpHeaders.AUTHORIZATION, bearer("admin")))
                    .andExpect(status().isBadRequest());
        } finally {
            Desafio d = desafios.findById(id).orElseThrow();
            d.setPublicado(true);
            desafios.save(d);
        }
    }
}
