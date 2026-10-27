# gamificacao-api

Microserviço de gamificação do Nexo: **XP e nível**, **ranking**, **atividades recentes**,
**desafios** e **quiz** do aluno. Porta padrão: **8084**.

Não tem login nem conta de usuário: quem autentica é o **usuario-api**, e este serviço só valida o
token que ele emite.

## Rodando localmente

```bash
cp .env.example .env          # preencha NEXO_JWT_SECRET e DB_PASSWORD
docker compose up -d postgres-gamificacao
./mvnw spring-boot:run
```

- `NEXO_JWT_SECRET` precisa ser **o mesmo do usuario-api**: é o que faz o token emitido por ele
  valer aqui.
- O banco é **próprio** (`gamificacao_db`, porta 5439 no host).
- Com `NEXO_SEED_ENABLED=true` (só dev, nunca com o perfil `prod`) nascem 12 jogadores, 6 desafios
  com quiz e as atividades do aluno de exemplo. **Ligue também o seed do usuario-api**: os
  jogadores usam os mesmos ids de aluno (1 a 12; o aluno 1 é a conta de usuário 3).

## Jogador: cópia local

O ranking precisa conhecer cada aluno (nome, foto) e o que ele escolheu esconder. Este serviço
guarda uma **cópia enxuta** (`Jogador`), com o mesmo id do aluno no usuario-api, mais os números
que são daqui (XP, ofensiva, tarefas). Turma e etapa de ensino **não** são guardadas: são do
pedagogico-api.

O usuario-api mantém a cópia em dia, **depois do commit**, quando um aluno é cadastrado ou muda de
nome, foto ou privacidade. É melhor esforço; para se recuperar, `POST
/api/admin/contas/sincronizar-jogadores` no usuario-api reenvia todos os alunos.

| Rota | Quem |
|---|---|
| `PUT /api/sync/alunos/{id}` | DIRETOR, ADMIN |
| `PATCH /api/sync/usuarios/{usuarioId}/perfil` | DIRETOR, ADMIN, ou o próprio dono |
| `PATCH /api/sync/usuarios/{usuarioId}/privacidade` | DIRETOR, ADMIN, ou o próprio dono |

## Pedagogico-api

Dois dados vêm de lá, com o **token do próprio aluno** (`GET /api/aluno/contexto`): o nome da
turma (dashboard, melhor esforço) e as matérias fora da etapa do aluno (para esconder desafios de
Química a quem está no fundamental, por exemplo). Se o pedagogico-api não responder, a lista de
desafios é **503** — mostrar tudo vazaria conteúdo da outra etapa — mas o dashboard abre sem a
turma. `NEXO_PEDAGOGICO_ENABLED=false` desliga a consulta (nada é filtrado).

## Rotas do aluno e do admin

| Rota | Quem |
|---|---|
| `GET /api/aluno/dashboard` | ALUNO |
| `GET /api/aluno/desafios`, `GET /{id}` | ALUNO |
| `POST /api/aluno/desafios/{id}/iniciar`, `/concluir` | ALUNO |
| `GET /api/aluno/desafios/{id}/quiz`, `POST /{id}/quiz/finalizar` | ALUNO |
| `GET /api/admin/catalogo/desafios`, `PATCH /{id}/publicado` | ADMIN |

Regras que valem a pena saber: o XP de um desafio é creditado **uma única vez**; o quiz só aprova
com 100% de acerto, e tentativa reprovada não revela placar nem gabarito; desafio despublicado
responde 404; quem desliga "Exibir no ranking" sai do ranking e da contagem de posições, e quem
deixa o perfil privado aparece sem foto para os colegas.

## Testes

`./mvnw verify` — usam H2 em memória (perfil `teste`), sem Postgres nem pedagogico-api.
