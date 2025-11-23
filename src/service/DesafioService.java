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

                String userId = linha.substring(0, p1);
                String sessionId = linha.substring(p1 + 1, p2);
                String actionText = linha.substring(p2 + 1);

                Acao acao;
                try {
                    acao = Acao.valueOf(actionText);
                } catch (Exception e) {
                    continue;
                }

                Deque<String> pilha = pilhas.computeIfAbsent(userId, k -> new ArrayDeque<>(4));

                switch (acao) {

                    case LOGIN -> {
                        if (!pilha.isEmpty()) {
                            sessoesInvalidas.add(sessionId);
                        }
                        pilha.push(sessionId);
                    }

                    case LOGOUT -> {
                        if (pilha.isEmpty()) {
                            sessoesInvalidas.add(sessionId);
                        } else if (!Objects.equals(pilha.peek(), sessionId)) {
                            sessoesInvalidas.add(sessionId);
                        } else {
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
        Queue<String> fila = new ArrayDeque<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            while ((linha = reader.readLine()) != null) {

                int p1 = linha.indexOf(',');
                int p2 = (p1 == -1 ? -1 : linha.indexOf(',', p1 + 1));

                if (p1 == -1 || p2 == -1) {
                    continue;
                }

                String logSessionId = linha.substring(p1 + 1, p2);

                if (logSessionId.equals(sessionId)) {
                    String actionType = linha.substring(p2 + 1);
                    fila.add(actionType);
                }
            }
        }
        List<String> resultado = new ArrayList<>(fila.size());
        while (!fila.isEmpty()) {
            resultado.add(fila.poll());
        }

        return resultado;
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {
        return List.of();
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {

        List<long[]> eventos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;

            while ((linha = br.readLine()) != null) {
                String[] p = linha.split(",");

                if (p.length != 7) continue;

                long timestamp;
                long bytes;

                try {
                    timestamp = Long.parseLong(p[0]);
                    bytes = Long.parseLong(p[6]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (bytes > 0) {
                    eventos.add(new long[]{timestamp, bytes});
                }
            }
        }

        Map<Long, Long> resultado = new HashMap<>();
        Deque<long[]> stack = new ArrayDeque<>();

        for (int i = eventos.size() - 1; i >= 0; i--) {
            long ts = eventos.get(i)[0];
            long bytes = eventos.get(i)[1];

            while (!stack.isEmpty() && stack.peek()[1] <= bytes) {
                stack.pop();
            }
            if (!stack.isEmpty()) {
                resultado.put(ts, stack.peek()[0]);
            }
            stack.push(new long[]{ts, bytes});
        }

        return resultado;
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(
            String caminhoArquivo,
            String recursoInicial,
            String recursoAlvo
    ) throws IOException {

        Map<String, List<String>> grafo = new HashMap<>();

        Map<String, List<String[]>> porSessao = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;

            while ((linha = br.readLine()) != null) {

                String[] p = linha.split(",");
                if (p.length != 7) continue;

                String sessionId = p[2];
                String target = p[4];

                porSessao.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(p);
            }
        }
        for (List<String[]> sessao : porSessao.values()) {

            sessao.sort(Comparator.comparingLong(a -> Long.parseLong(a[0])));

            for (int i = 0; i < sessao.size() - 1; i++) {
                String origem = sessao.get(i)[4];
                String destino = sessao.get(i + 1)[4];

                grafo.computeIfAbsent(origem, k -> new ArrayList<>()).add(destino);
            }
        }

        Queue<String> fila = new ArrayDeque<>();
        Map<String, String> pred = new HashMap<>();
        fila.add(recursoInicial);
        pred.put(recursoInicial, null);
        while (!fila.isEmpty()) {
            String atual = fila.poll();
            if (atual.equals(recursoAlvo)) {
                break;
            }
            for (String viz : grafo.getOrDefault(atual, List.of())) {
                if (!pred.containsKey(viz)) {
                    pred.put(viz, atual);
                    fila.add(viz);
                }
            }
        }
        if (!pred.containsKey(recursoAlvo)) {
            return Optional.empty();
        }
        List<String> caminho = new ArrayList<>();
        String cur = recursoAlvo;
        while (cur != null) {
            caminho.add(cur);
            cur = pred.get(cur);
        }
        Collections.reverse(caminho);
        return Optional.of(caminho);
    }
}
