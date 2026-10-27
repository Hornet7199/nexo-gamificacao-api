package com.nexo.gamificacaoapi.web;

import com.nexo.gamificacaoapi.api.ApiException;
import com.nexo.gamificacaoapi.domain.Desafio;
import com.nexo.gamificacaoapi.repository.DesafioRepository;
import com.nexo.gamificacaoapi.security.UsuarioAutenticado;
import com.nexo.gamificacaoapi.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de desafios para a administração: ver tudo e tirar do ar / devolver. */
@RestController
@RequestMapping("/api/admin/catalogo/desafios")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogoAdminController {

    public record DesafioAdminDTO(Long id, String titulo, String materia, String nivel,
                                  int xp, int tempoMin, boolean publicado) {
        static DesafioAdminDTO of(Desafio d) {
            return new DesafioAdminDTO(d.getId(), d.getTitulo(), d.getMateria(), d.getNivel(),
                    d.getXp(), d.getTempoMin(), d.isPublicado());
        }
    }

    public record AtualizarPublicadoRequest(Boolean publicado) {}

    private final DesafioRepository desafios;
    private final AuditoriaService auditoria;

    public CatalogoAdminController(DesafioRepository desafios, AuditoriaService auditoria) {
        this.desafios = desafios;
        this.auditoria = auditoria;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DesafioAdminDTO> listar() {
        return desafios.findAllByOrderByMateriaAscTituloAsc().stream().map(DesafioAdminDTO::of).toList();
    }

    @PatchMapping("/{id}/publicado")
    @Transactional
    public DesafioAdminDTO publicar(@PathVariable Long id, @RequestBody AtualizarPublicadoRequest request,
                                    @AuthenticationPrincipal UsuarioAutenticado operador) {
        if (request == null || request.publicado() == null) {
            throw ApiException.badRequest("Informe se o desafio fica publicado.");
        }
        Desafio desafio = desafios.findById(id)
                .orElseThrow(() -> ApiException.notFound("Desafio não encontrado."));
        if (desafio.isPublicado() == request.publicado()) {
            return DesafioAdminDTO.of(desafio); // idempotente
        }
        desafio.setPublicado(request.publicado());
        desafios.save(desafio);
        auditoria.registrar(operador.nome(), AuditoriaService.Tipo.ALTERACAO,
                request.publicado() ? "Desafio publicado" : "Desafio despublicado",
                desafio.getTitulo(), null);
        return DesafioAdminDTO.of(desafio);
    }
}
