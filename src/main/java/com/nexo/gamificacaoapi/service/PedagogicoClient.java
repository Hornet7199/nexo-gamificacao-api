package com.nexo.gamificacaoapi.service;

import com.nexo.gamificacaoapi.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Único ponto em que este serviço fala com o pedagogico-api: a turma e a etapa de ensino do aluno
 * são dele, e a gamificação só precisa saber duas coisas — o nome da turma, para o dashboard, e
 * quais matérias <b>não</b> são da etapa do aluno, para esconder os desafios delas.
 *
 * <p>A chamada repassa o {@code Authorization} do próprio aluno (o pedagogico-api só entrega esse
 * contexto a quem está logado como aluno, e para ele mesmo). Com
 * {@code nexo.pedagogico.enabled=false} nenhuma chamada é feita e nada é filtrado.
 */
@Component
public class PedagogicoClient {

    private static final Logger log = LoggerFactory.getLogger(PedagogicoClient.class);

    /**
     * O que o pedagogico-api diz do aluno logado.
     *
     * @param turma                nome da turma, ou nulo se ele ainda não tem
     * @param etapa                {@code FUNDAMENTAL} ou {@code MEDIO}
     * @param materiasForaDaEtapa  nomes de matérias do catálogo que o aluno não cursa
     */
    public record ContextoAluno(String turma, String etapa, List<String> materiasForaDaEtapa) {

        /** Sem informação: nenhum filtro, turma desconhecida. */
        public static ContextoAluno semRestricao() {
            return new ContextoAluno(null, null, List.of());
        }

        public List<String> materiasForaDaEtapa() {
            return materiasForaDaEtapa == null ? List.of() : materiasForaDaEtapa;
        }
    }

    private final boolean habilitado;
    private final RestClient http;

    public PedagogicoClient(@Value("${nexo.pedagogico.enabled:true}") boolean habilitado,
                            @Value("${nexo.pedagogico.url:http://localhost:8082}") String url,
                            @Value("${nexo.pedagogico.timeout-ms:3000}") int timeoutMs) {
        this.habilitado = habilitado;
        this.http = ClienteHttp.criar(url, timeoutMs);
    }

    /** Lança 503 se o pedagogico-api não responder. */
    public ContextoAluno contextoDoAluno() {
        if (!habilitado) return ContextoAluno.semRestricao();
        try {
            ContextoAluno c = http.get().uri("/api/aluno/contexto")
                    .header(HttpHeaders.AUTHORIZATION, ClienteHttp.tokenDaRequisicao())
                    .retrieve()
                    .body(ContextoAluno.class);
            return c != null ? c : ContextoAluno.semRestricao();
        } catch (RestClientException e) {
            log.warn("pedagogico-api indisponível ao consultar o contexto do aluno: {}", e.getMessage());
            throw ApiException.servicoIndisponivel(
                    "Não foi possível consultar a etapa de ensino do aluno. Tente novamente em instantes.");
        }
    }

    /** Melhor esforço: quando só enfeita a resposta (o nome da turma), a falha não derruba a tela. */
    public ContextoAluno contextoDoAlunoOuVazio() {
        try {
            return contextoDoAluno();
        } catch (ApiException e) {
            return ContextoAluno.semRestricao();
        }
    }
}
