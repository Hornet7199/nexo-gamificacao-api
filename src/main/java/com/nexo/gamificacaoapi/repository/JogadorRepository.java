package com.nexo.gamificacaoapi.repository;

import com.nexo.gamificacaoapi.domain.Jogador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JogadorRepository extends JpaRepository<Jogador, Long> {

    Optional<Jogador> findByUsuarioId(Long usuarioId);

    /**
     * Só as colunas que o ranking exibe, já ordenadas pelo banco. Quem desligou "Exibir no
     * ranking" não entra: nem na lista dos outros, nem na conta da posição de ninguém.
     *
     * <p>O dashboard mostra 5 posições, mas precisa varrer a escola inteira para achar a
     * colocação do aluno logado — com {@code findAll()} isso trazia toda a tabela como entidade
     * gerenciada, cada uma com o snapshot de dirty-checking do Hibernate junto. O desempate por
     * id dá uma ordem estável a quem tem o mesmo XP.
     */
    interface RankingXp {
        Long getId();
        String getNome();
        int getXpTotal();
        String getFoto();
        boolean isPerfilPublico();
    }

    @Query("""
           select j.id as id, j.nome as nome, j.xpTotal as xpTotal, j.foto as foto,
                  j.perfilPublico as perfilPublico
           from Jogador j
           where j.exibirNoRanking = true
           order by j.xpTotal desc, j.id asc
           """)
    List<RankingXp> rankingPorXp();
}
