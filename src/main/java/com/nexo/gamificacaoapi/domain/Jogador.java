package com.nexo.gamificacaoapi.domain;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * O aluno como a gamificação o conhece: quem é (nome e foto, para o ranking), o que acumulou
 * (XP, ofensiva, tarefas) e o que escolheu esconder. Uma cópia enxuta de quem vive no
 * usuario-api, mais os números que são daqui.
 *
 * <p>O {@code id} <b>não é gerado aqui</b>: é o id do aluno no usuario-api, o mesmo que o
 * pedagogico-api usa. A linha nasce quando o usuario-api avisa do cadastro e é atualizada quando
 * nome, foto ou privacidade mudam lá (ver {@code SincronizacaoController}). Turma e etapa de
 * ensino <b>não</b> são guardadas: pertencem ao pedagógico, que responde quando perguntado.
 */
@Entity
@Table(name = "jogadores",
       // O ranking ordena a escola inteira por XP a cada abertura do dashboard.
       indexes = @Index(name = "idx_jogadores_xp", columnList = "xp_total"))
@Getter
@Setter
public class Jogador {

    /** Id do aluno no usuario-api. */
    @Id
    private Long id;

    /** Id da conta de acesso no usuario-api — é o que vem no claim {@code uid} do token. */
    @Column(unique = true)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    private String foto;

    // ── Gamificação ──
    private int xpTotal;
    private int xpSemana;
    private int metaSemanalXp = 1000;
    private int ofensivaDias;
    private int tarefasFeitasHoje;
    private int tarefasHoje;

    // ── Privacidade (escolhas do aluno em Configurações → Privacidade) ──

    /** Aparece no ranking dos outros alunos. Desligada, também não entra na conta da posição de ninguém. */
    @Column(nullable = false)
    private boolean exibirNoRanking = true;

    /** Outros alunos veem a foto (hoje, o ranking é o único lugar em que um aluno vê outro). */
    @Column(nullable = false)
    private boolean perfilPublico = true;
}
