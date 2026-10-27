package com.nexo.gamificacaoapi.web;

import com.nexo.gamificacaoapi.security.UsuarioAutenticado;
import com.nexo.gamificacaoapi.service.DashboardService;
import com.nexo.gamificacaoapi.service.DesafioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gamificação do aluno logado: dashboard, desafios e quiz. */
@RestController
@RequestMapping("/api/aluno")
@PreAuthorize("hasRole('ALUNO')")
public class DesafiosController {

    private final DesafioService desafios;
    private final DashboardService dashboard;

    public DesafiosController(DesafioService desafios, DashboardService dashboard) {
        this.desafios = desafios;
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public DashboardService.AlunoDashboardDTO dashboard(@AuthenticationPrincipal UsuarioAutenticado p) {
        return dashboard.montar(p.id());
    }

    @GetMapping("/desafios")
    public DesafioService.DesafiosResponse listar(@AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.listar(p.id());
    }

    @GetMapping("/desafios/{id}")
    public DesafioService.DesafioDTO obter(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.obter(p.id(), id);
    }

    @PostMapping("/desafios/{id}/iniciar")
    public DesafioService.DesafioDTO iniciar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.iniciar(p.id(), id);
    }

    @PostMapping("/desafios/{id}/concluir")
    public DesafioService.DesafioDTO concluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.concluir(p.id(), id);
    }

    @GetMapping("/desafios/{id}/quiz")
    public List<DesafioService.QuizPerguntaDTO> quiz(@PathVariable Long id,
                                                     @AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.listarPerguntas(p.id(), id);
    }

    @PostMapping("/desafios/{id}/quiz/finalizar")
    public DesafioService.QuizResultadoDTO finalizar(@PathVariable Long id,
                                                     @Valid @RequestBody(required = false) DesafioService.FinalizarQuizRequest corpo,
                                                     @AuthenticationPrincipal UsuarioAutenticado p) {
        return desafios.finalizarQuiz(p.id(), id, corpo);
    }
}
