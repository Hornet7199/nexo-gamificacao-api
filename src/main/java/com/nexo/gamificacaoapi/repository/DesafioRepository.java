package com.nexo.gamificacaoapi.repository;

import com.nexo.gamificacaoapi.domain.Desafio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DesafioRepository extends JpaRepository<Desafio, Long> {

    /** Catálogo do administrador: publicados e despublicados, agrupados por matéria. */
    List<Desafio> findAllByOrderByMateriaAscTituloAsc();

    /** Catálogo do aluno: só o que está no ar. */
    List<Desafio> findByPublicadoIsNullOrPublicadoTrue();
}
