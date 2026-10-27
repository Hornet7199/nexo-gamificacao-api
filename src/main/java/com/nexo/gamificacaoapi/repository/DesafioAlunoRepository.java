package com.nexo.gamificacaoapi.repository;

import com.nexo.gamificacaoapi.domain.DesafioAluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesafioAlunoRepository extends JpaRepository<DesafioAluno, Long> {

    List<DesafioAluno> findByJogadorId(Long jogadorId);

    Optional<DesafioAluno> findByJogadorIdAndDesafioId(Long jogadorId, Long desafioId);
}
