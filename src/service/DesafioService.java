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
        List<String> linhaDoTempo = new ArrayList<>(fila.size());
        while (!fila.isEmpty()) {
            linhaDoTempo.add(fila.poll());
        }

        return linhaDoTempo;
    }

    @Override
    public List<Alerta> priorizarAlertas(String caminhoArquivo, int n) throws IOException {
        // Caso trivial
        if (n <= 0) return Collections.emptyList();

        // PriorityQueue em ordem decrescente de severidade
        PriorityQueue<Alerta> filaPrioridade = new PriorityQueue<>(
                (a1, a2) -> Integer.compare(a2.getSeverityLevel(), a1.getSeverityLevel())
        );

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            while ((linha = reader.readLine()) != null) {

                // timestamp,userId,sessionId,actionType,targetResource,severityLevel,bytesTransferred

                int p1 = linha.indexOf(',');
                int p2 = linha.indexOf(',', p1 + 1);
                int p3 = linha.indexOf(',', p2 + 1);
                int p4 = linha.indexOf(',', p3 + 1);
                int p5 = linha.indexOf(',', p4 + 1);
                int p6 = linha.indexOf(',', p5 + 1);

                if (p1 < 0 || p2 < 0 || p3 < 0 || p4 < 0 || p5 < 0 || p6 < 0)
                    continue; // linha inválida

                long timestamp = Long.parseLong(linha.substring(0, p1));
                String userId = linha.substring(p1 + 1, p2);
                String sessionId = linha.substring(p2 + 1, p3);
                String actionType = linha.substring(p3 + 1, p4);
                String targetResource = linha.substring(p4 + 1, p5);
                int severityLevel = Integer.parseInt(linha.substring(p5 + 1, p6));
                long bytesTransferred = Long.parseLong(linha.substring(p6 + 1));

                Alerta alerta = new Alerta(timestamp, userId, sessionId, actionType, targetResource, severityLevel,
                        bytesTransferred);

                // Adicionar na priority queue
                filaPrioridade.add(alerta);
            }
        }

        // Extrair até N elementos
        List<Alerta> alertas = new ArrayList<>(n);

        for (int i = 0; i < n && !filaPrioridade.isEmpty(); i++) {
            alertas.add(filaPrioridade.poll());
        }

        return alertas;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        List<long[]> eventos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;

            while ((linha = reader.readLine()) != null) {

                int idx1 = linha.indexOf(',');
                if (idx1 == -1) continue;

                long timestamp;
                try {
                    timestamp = Long.parseLong(linha.substring(0, idx1));
                } catch (NumberFormatException e) {
                    continue; // linha inválida
                }

                // Agora precisamos do campo 7 (bytesTransferred)
                // Vamos avançar 5 vírgulas adicionais até chegar nele.
                int start = idx1 + 1;
                int virgulasRestantes = 5;

                while (virgulasRestantes > 0) {
                    start = linha.indexOf(',', start) + 1;
                    if (start == 0) { // não achou a vírgula
                        start = -1;
                        break;
                    }
                    virgulasRestantes--;
                }

                if (start == -1) continue;

                // Agora achar a próxima vírgula que fecha o campo 6
                int end = linha.indexOf(',', start);
                if (end == -1) end = linha.length();

                long bytes;
                try {
                    bytes = Long.parseLong(linha.substring(start, end));
                } catch (NumberFormatException e) {
                    continue; // bytes inválidos
                }

                if (bytes > 0) {
                    eventos.add(new long[]{timestamp, bytes});
                }
            }
        }

        int size = eventos.size();
        if (size == 0) return Collections.emptyMap();

        Map<Long, Long> encontrados = new HashMap<>(size / 2);
        ArrayDeque<long[]> stack = new ArrayDeque<>();

        // Processa em ordem reversa (Next Greater Element)
        for (int i = size - 1; i >= 0; i--) {

            long ts = eventos.get(i)[0];
            long bytes = eventos.get(i)[1];

            while (!stack.isEmpty() && stack.peek()[1] <= bytes) {
                stack.pop();
            }

            if (!stack.isEmpty()) {
                encontrados.put(ts, stack.peek()[0]);
            }

            stack.push(new long[]{ts, bytes});
        }

        return encontrados;
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(
            String caminhoArquivo,
            String recursoInicial,
            String recursoAlvo
    ) throws IOException {

        Map<String, List<String>> grafo = new HashMap<>();

        Map<String, List<String[]>> porSessao = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;

            while ((linha = reader.readLine()) != null) {

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