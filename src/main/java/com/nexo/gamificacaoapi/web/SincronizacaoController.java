package com.nexo.gamificacaoapi.web;

import com.nexo.gamificacaoapi.security.UsuarioAutenticado;
import com.nexo.gamificacaoapi.service.SincronizacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Canal pelo qual o usuario-api avisa a gamificação de quem se cadastrou ou mudou. Não é usado
 * pelo front; a chamada vem com o token do próprio operador.
 */
@RestController
@RequestMapping("/api/sync")
public class SincronizacaoController {

    private final SincronizacaoService sincronizacao;

    public SincronizacaoController(SincronizacaoService sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    public record AlunoSync(Long usuarioId,
                            @NotBlank(message = "Informe o nome.") @Size(max = 150) String nome,
                            @Size(max = 255) String foto,
                            Boolean exibirNoRanking,
                            Boolean perfilPublico) {}

    public record PerfilSync(@Size(max = 150) String nome, @Size(max = 255) String foto) {}

    public record PrivacidadeSync(Boolean exibirNoRanking, Boolean perfilPublico) {}

    @PutMapping("/alunos/{id}")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aluno(@PathVariable Long id, @Valid @RequestBody AlunoSync corpo,
                      @AuthenticationPrincipal UsuarioAutenticado operador) {
        sincronizacao.sincronizarAluno(id, corpo.usuarioId(), corpo.nome(), corpo.foto(),
                corpo.exibirNoRanking(), corpo.perfilPublico(), operador.nome());
    }

    /** Quem pode: o próprio dono da conta ou a gestão — senão qualquer um trocaria o nome de outro. */
    @PatchMapping("/usuarios/{usuarioId}/perfil")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN') or #usuarioId == authentication.principal.id()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void perfil(@PathVariable Long usuarioId, @Valid @RequestBody PerfilSync corpo) {
        sincronizacao.sincronizarPerfil(usuarioId, corpo.nome(), corpo.foto());
    }

    @PatchMapping("/usuarios/{usuarioId}/privacidade")
    @PreAuthorize("hasAnyRole('DIRETOR','ADMIN') or #usuarioId == authentication.principal.id()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void privacidade(@PathVariable Long usuarioId, @RequestBody PrivacidadeSync corpo) {
        sincronizacao.sincronizarPrivacidade(usuarioId, corpo.exibirNoRanking(), corpo.perfilPublico());
    }
}
