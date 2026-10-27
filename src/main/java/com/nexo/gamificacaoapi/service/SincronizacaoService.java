package com.nexo.gamificacaoapi.service;

import com.nexo.gamificacaoapi.domain.Jogador;
import com.nexo.gamificacaoapi.repository.JogadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantém a cópia local do aluno (o {@link Jogador}) em dia com o usuario-api, que é a fonte de
 * verdade de nome, foto e privacidade. Tudo é upsert pelo id do aluno no usuario-api, então
 * repetir a chamada não duplica nada. O XP e o resto da gamificação nunca são tocados aqui.
 */
@Service
public class SincronizacaoService {

    private final JogadorRepository jogadores;
    private final AuditoriaService auditoria;

    public SincronizacaoService(JogadorRepository jogadores, AuditoriaService auditoria) {
        this.jogadores = jogadores;
        this.auditoria = auditoria;
    }

    /** Flag nula = não mexe (aluno novo nasce com as duas ligadas). */
    @Transactional
    public void sincronizarAluno(Long id, Long usuarioId, String nome, String foto,
                                 Boolean exibirNoRanking, Boolean perfilPublico, String operador) {
        boolean novo = !jogadores.existsById(id);
        Jogador j = jogadores.findById(id).orElseGet(Jogador::new);
        j.setId(id);
        j.setUsuarioId(usuarioId);
        j.setNome(nome.trim());
        j.setFoto(foto);
        if (exibirNoRanking != null) j.setExibirNoRanking(exibirNoRanking);
        if (perfilPublico != null) j.setPerfilPublico(perfilPublico);
        jogadores.save(j);
        if (novo) {
            auditoria.registrar(operador, AuditoriaService.Tipo.ALTERACAO,
                    "Aluno sincronizado", "Aluno: " + j.getNome(), null);
        }
    }

    /** Conta sem registro aqui (professor, diretor) é ignorada: o usuario-api não sabe quem joga. */
    @Transactional
    public void sincronizarPerfil(Long usuarioId, String nome, String foto) {
        jogadores.findByUsuarioId(usuarioId).ifPresent(j -> {
            if (nome != null && !nome.isBlank()) j.setNome(nome.trim());
            j.setFoto(foto);
            jogadores.save(j);
        });
    }

    @Transactional
    public void sincronizarPrivacidade(Long usuarioId, Boolean exibirNoRanking, Boolean perfilPublico) {
        jogadores.findByUsuarioId(usuarioId).ifPresent(j -> {
            if (exibirNoRanking != null) j.setExibirNoRanking(exibirNoRanking);
            if (perfilPublico != null) j.setPerfilPublico(perfilPublico);
            jogadores.save(j);
        });
    }
}
