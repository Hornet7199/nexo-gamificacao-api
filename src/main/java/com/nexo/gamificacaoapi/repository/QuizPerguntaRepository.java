package com.nexo.gamificacaoapi.repository;

import com.nexo.gamificacaoapi.domain.QuizPergunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizPerguntaRepository extends JpaRepository<QuizPergunta, Long> {
    List<QuizPergunta> findByDesafioIdOrderById(Long desafioId);
}
