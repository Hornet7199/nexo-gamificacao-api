package com.nexo.gamificacaoapi.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Progresso de um aluno em um desafio. */
@Entity
@Table(name = "desafios_aluno",
       // Composto e nesta ordem: serve tanto findByJogadorId (prefixo) quanto
       // findByJogadorIdAndDesafioId, que roda a cada abertura e conclusão de desafio.
       indexes = @Index(name = "idx_desafios_aluno_jogador_desafio", columnList = "jogador_id, desafio_id"))
@Getter
public class DesafioAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Jogador jogador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Desafio desafio;

    /** ABERTO | PROGRESSO | CONCLUIDO */
    @Setter
    private String status = "ABERTO";

    @Setter
    private int progresso;

    /**
     * Preenchidos apenas quando o aluno gabarita o quiz. Tentativa reprovada não grava placar —
     * assim reabrir o desafio não revela nada da tentativa anterior.
     */
    @Setter
    private Integer acertos;

    @Setter
    private Integer totalPerguntas;

    /** Quantas vezes o aluno já enviou o quiz deste desafio (reprovadas incluídas). */
    private int tentativas;

    public DesafioAluno() {}

    public DesafioAluno(Jogador jogador, Desafio desafio, String status, int progresso) {
        this.jogador = jogador;
        this.desafio = desafio;
        this.status = status;
        this.progresso = progresso;
    }

    public void setTentativas(int tentativas) { this.tentativas = tentativas; }
}
