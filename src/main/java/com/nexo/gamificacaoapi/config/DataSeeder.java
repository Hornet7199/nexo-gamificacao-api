package com.nexo.gamificacaoapi.config;

import com.nexo.gamificacaoapi.domain.AtividadeAluno;
import com.nexo.gamificacaoapi.domain.Desafio;
import com.nexo.gamificacaoapi.domain.DesafioAluno;
import com.nexo.gamificacaoapi.domain.Jogador;
import com.nexo.gamificacaoapi.repository.AtividadeAlunoRepository;
import com.nexo.gamificacaoapi.repository.DesafioAlunoRepository;
import com.nexo.gamificacaoapi.repository.DesafioRepository;
import com.nexo.gamificacaoapi.repository.JogadorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Popula o banco de desenvolvimento com os jogadores, desafios e atividades de exemplo. Roda só
 * com {@code nexo.seed.enabled=true} e em banco vazio.
 *
 * <p>Não cria conta de acesso: as contas são do usuario-api. Os jogadores usam os <b>mesmos
 * ids</b> dos alunos do seed do usuario-api (1 a 12), e o aluno 1 (Gabriel) é o da conta de
 * usuário 3. Os seeds precisam andar juntos: mudar a ordem ou a lista de um sem o outro
 * desalinha os ids.
 */
@Component
@ConditionalOnProperty(name = "nexo.seed.enabled", havingValue = "true")
@Order(2)
public class DataSeeder implements CommandLineRunner {

    /** Id da conta do aluno de exemplo no seed do usuario-api. */
    private static final long USUARIO_ALUNO = 3L;

    /** Nome e engajamento (0-100) na ordem dos ids 1 a 12 — o engajamento só calibra o XP. */
    private static final Object[][] ALUNOS = {
            {"Gabriel Mendes", 88}, {"Ana Beatriz Souza", 92}, {"Carlos Eduardo Lima", 45},
            {"Mariana Ferreira", 78}, {"João Pedro Santos", 25}, {"Larissa Oliveira", 83},
            {"Rafael Costa", 52}, {"Beatriz Almeida", 95}, {"Lucas Martins", 38},
            {"Julia Rodrigues", 72}, {"Pedro Henrique Silva", 60}, {"Camila Barbosa", 90},
    };

    private final JogadorRepository jogadores;
    private final DesafioRepository desafios;
    private final DesafioAlunoRepository desafiosAluno;
    private final AtividadeAlunoRepository atividadesAluno;

    public DataSeeder(JogadorRepository jogadores, DesafioRepository desafios,
                      DesafioAlunoRepository desafiosAluno, AtividadeAlunoRepository atividadesAluno) {
        this.jogadores = jogadores;
        this.desafios = desafios;
        this.desafiosAluno = desafiosAluno;
        this.atividadesAluno = atividadesAluno;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (jogadores.count() > 0) return;

        Random random = new Random(42);
        Jogador gabriel = null;
        long id = 1;
        for (Object[] d : ALUNOS) {
            int engajamento = (int) d[1];
            Jogador j = new Jogador();
            j.setId(id);
            j.setUsuarioId(id == 1 ? USUARIO_ALUNO : null);
            j.setNome((String) d[0]);
            // XP correlacionado ao engajamento
            j.setXpTotal(1500 + engajamento * 35 + random.nextInt(400));
            j.setXpSemana(300 + random.nextInt(700));
            j.setOfensivaDias(random.nextInt(14));
            j.setTarefasHoje(5);
            j.setTarefasFeitasHoje(3 + random.nextInt(3));
            jogadores.save(j);
            if (id == 1) gabriel = j;
            id++;
        }

        Instant agora = Instant.now();
        atividadesAluno.save(new AtividadeAluno(gabriel, "Exercícios de Frações", "Matemática", 150, 100, "🧮", agora.minus(2, ChronoUnit.HOURS)));
        atividadesAluno.save(new AtividadeAluno(gabriel, "Revisão de Ecossistemas", "Biologia", 80, 45, "🧬", agora.minus(5, ChronoUnit.HOURS)));
        atividadesAluno.save(new AtividadeAluno(gabriel, "Redação sobre Brasil Colônia", "História", 200, 70, "📜", agora.minus(1, ChronoUnit.DAYS)));
        atividadesAluno.save(new AtividadeAluno(gabriel, "Quiz de Vocabulário", "Inglês", 120, 90, "🇺🇸", agora.minus(2, ChronoUnit.DAYS)));

        // Perguntas de quiz destes desafios são semeadas à parte, em QuizPerguntasSeed.
        Desafio d1 = desafios.save(new Desafio("Quiz: Fotossíntese", "Biologia", "MEDIO", 150, 15));
        Desafio d2 = desafios.save(new Desafio("Funções Trigonométricas", "Matemática", "DIFICIL", 250, 30));
        desafios.save(new Desafio("Present Perfect", "Inglês", "FACIL", 100, 20));
        desafios.save(new Desafio("Revolução Industrial", "História", "MEDIO", 180, 25));
        desafios.save(new Desafio("Ecossistemas e Biomas", "Biologia", "FACIL", 120, 15));
        desafios.save(new Desafio("Derivadas e Integrais", "Matemática", "DIFICIL", 300, 40));

        DesafioAluno da1 = new DesafioAluno(gabriel, d1, "CONCLUIDO", 100);
        da1.setAcertos(3);
        da1.setTotalPerguntas(3);
        desafiosAluno.save(da1);
        desafiosAluno.save(new DesafioAluno(gabriel, d2, "PROGRESSO", 40));
    }
}
