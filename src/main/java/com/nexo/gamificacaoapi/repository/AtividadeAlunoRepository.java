package com.nexo.gamificacaoapi.repository;

import com.nexo.gamificacaoapi.domain.AtividadeAluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtividadeAlunoRepository extends JpaRepository<AtividadeAluno, Long> {

    List<AtividadeAluno> findTop6ByJogadorIdOrderByCriadaEmDesc(Long jogadorId);
}
