package service;

import br.edu.icev.aed.forense.Alerta;
import br.edu.icev.aed.forense.AnaliseForenseAvancada;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class DesafioService implements AnaliseForenseAvancada {
    private enum Acao {LOGIN, LOGOUT}

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivo) throws IOException {
        Map<String, Deque<String>> pilhas = new HashMap<>();
        Set<String> sessoesInvalidas = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            while ((linha = reader.readLine()) != null) {

                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);

                if (p1 == -1 || p2 == -1) continue; // linha inválida

                String userId    = linha.substring(0, p1);
                String sessionId = linha.substring(p1 + 1, p2);
                String actionText  = linha.substring(p2 + 1);

                Acao acao;
                try {
                    acao = Acao.valueOf(actionText);
                } catch (Exception e) {
                    continue; // ação inválida
                }

                Deque<String> pilha = pilhas.computeIfAbsent(userId, k -> new ArrayDeque<>(4));

                switch (acao) {

                    case LOGIN -> {
                        if (!pilha.isEmpty()) sessoesInvalidas.add(sessionId);
                        pilha.push(sessionId);
                    }

                    case LOGOUT -> {
                        if (pilha.isEmpty()){
                            sessoesInvalidas.add(sessionId);
                        }
                        else if (!Objects.equals(pilha.peek(), sessionId)){
                            sessoesInvalidas.add(sessionId);
                        }
                        else {
                            pilha.pop();
                        }
                    }
                }
            }
        }
        pilhas.values().forEach(sessoesInvalidas::addAll);
        return sessoesInvalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivo, String sessionId) throws IOException {
        return List.of();
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {
        return List.of();
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        return Map.of();
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo) throws IOException {
        return Optional.empty();
    }
}
