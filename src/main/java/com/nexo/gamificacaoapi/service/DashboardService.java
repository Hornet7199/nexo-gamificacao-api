package com.nexo.gamificacaoapi.service;

import com.nexo.gamificacaoapi.api.ApiException;
import com.nexo.gamificacaoapi.domain.Jogador;
import com.nexo.gamificacaoapi.repository.AtividadeAlunoRepository;
import com.nexo.gamificacaoapi.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o dashboard de gamificação do aluno (XP, ofensiva, ranking, tarefas, atividades
 * recentes). O nome da turma não é daqui: vem do pedagogico-api, em melhor esforço.
 */
@Service
public class DashboardService {

    public record AtividadeDTO(String titulo, String materia, int xp, int progresso, String icone, Instant criadaEm) {}

    public record RankingItemDTO(int posicao, String nome, int xp, String foto, boolean isMe) {}

    public record AlunoDashboardDTO(String nome, int xpSemana, int metaSemanalXp, int ofensivaDias,
                                    int posicao, int totalAlunos, String turmaNome,
                                    int tarefasFeitasHoje, int tarefasHoje, int xpTotal, int nivel,
                                    List<AtividadeDTO> atividades, List<RankingItemDTO> ranking,
                                    boolean foraDoRanking) {}

    private final JogadorRepository jogadores;
    private final AtividadeAlunoRepository atividades;
    private final PedagogicoClient pedagogico;

    public DashboardService(JogadorRepository jogadores, AtividadeAlunoRepository atividades,
                            PedagogicoClient pedagogico) {
        this.jogadores = jogadores;
        this.atividades = atividades;
        this.pedagogico = pedagogico;
    }

    /** Nível derivado do XP total (1 nível a cada 400 XP). */
    private static int nivelDe(int xpTotal) {
        return 1 + xpTotal / 400;
    }

    @Transactional(readOnly = true)
    public AlunoDashboardDTO montar(Long usuarioId) {
        Jogador jogador = jogadores.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.notFound("Aluno não encontrado para o usuário logado."));

        // Já vem sem quem desligou "Exibir no ranking" (ver JogadorRepository.rankingPorXp).
        List<JogadorRepository.RankingXp> ranking = jogadores.rankingPorXp();

        // Quem se tirou do ranking não tem posição nele: 0 + o sinal explícito, que a tela
        // usa para explicar em vez de mostrar "#0".
        boolean foraDoRanking = !jogador.isExibirNoRanking();
        int posicao = 0;
        if (!foraDoRanking) {
            posicao = 1;
            for (int i = 0; i < ranking.size(); i++) {
                if (ranking.get(i).getId().equals(jogador.getId())) {
                    posicao = i + 1;
                    break;
                }
            }
        }

        int topN = Math.min(5, ranking.size());
        List<RankingItemDTO> topRanking = new ArrayList<>(topN + 1);
        for (int i = 0; i < topN; i++) {
            JogadorRepository.RankingXp a = ranking.get(i);
            boolean sou = a.getId().equals(jogador.getId());
            topRanking.add(new RankingItemDTO(i + 1, a.getNome(), a.getXpTotal(), fotoVisivel(a, sou), sou));
        }
        // Garante que o aluno logado apareça mesmo fora do top 5 — a menos que tenha
        // escolhido não aparecer.
        if (!foraDoRanking && topRanking.stream().noneMatch(RankingItemDTO::isMe)) {
            topRanking.add(new RankingItemDTO(posicao, jogador.getNome(), jogador.getXpTotal(), jogador.getFoto(), true));
        }

        List<AtividadeDTO> feed = atividades.findTop6ByJogadorIdOrderByCriadaEmDesc(jogador.getId()).stream()
                .map(a -> new AtividadeDTO(a.getTitulo(), a.getMateria(), a.getXp(), a.getProgresso(),
                        a.getIcone(), a.getCriadaEm()))
                .toList();

        return new AlunoDashboardDTO(jogador.getNome(), jogador.getXpSemana(), jogador.getMetaSemanalXp(),
                jogador.getOfensivaDias(), posicao, ranking.size(),
                pedagogico.contextoDoAlunoOuVazio().turma(),
                jogador.getTarefasFeitasHoje(), jogador.getTarefasHoje(),
                jogador.getXpTotal(), nivelDe(jogador.getXpTotal()), feed, topRanking, foraDoRanking);
    }

    /**
     * A foto de um colega só sai se ele deixou o perfil público; a própria foto, sempre.
     * Sem a foto a tela cai no avatar padrão — o nome continua, porque quem está no
     * ranking aceitou aparecer nele.
     */
    private static String fotoVisivel(JogadorRepository.RankingXp a, boolean souEu) {
        return souEu || a.isPerfilPublico() ? a.getFoto() : null;
    }
}
