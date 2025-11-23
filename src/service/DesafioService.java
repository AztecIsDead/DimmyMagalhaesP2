package service;
import model.LogEntry;
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

                if (p1 == -1 || p2 == -1) continue;

                String userId    = linha.substring(0, p1);
                String sessionId = linha.substring(p1 + 1, p2);
                String actionText  = linha.substring(p2 + 1);

                Acao acao;
                try {
                    acao = Acao.valueOf(actionText);
                } catch (Exception e) {
                    continue;
                }

                Deque<String> pilha = pilhas.computeIfAbsent(userId, k -> new ArrayDeque<>(4));

                switch (acao) {

                    case LOGIN -> {
                        if (!pilha.isEmpty()) sessoesInvalidas.add(sessionId);
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
                }}}
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
        Map<Long, Long> picos = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(",");
                if (partes.length != 4) continue;
                try {
                    long timestamp = Long.parseLong(partes[0]);
                    long tamanho = Long.parseLong(partes[3]);
                    picos.merge(timestamp, tamanho, Long::sum);
                } catch (NumberFormatException e) {
                }}}

        return picos;
    }


    public Optional<List<String>> rastrearContaminacao(
            String caminhoArquivo,
            String recursoInicial,
            String recursoAlvo
    ) throws IOException {
        Map<String, Set<String>> grafo = new HashMap<>();
        Map<String, String> ultimoRecursoPorSessao = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] partes = linha.split(",");
                if (partes.length != 4) continue;
                String sessionOrUser = partes[1]; // userId funciona como agrupador no ZIP
                String recurso = partes[2];
                String anterior = ultimoRecursoPorSessao.put(sessionOrUser, recurso);
                if (anterior != null) {
                    grafo.computeIfAbsent(anterior, k -> new HashSet<>()).add(recurso);
                }}}
        Queue<List<String>> fila = new ArrayDeque<>();
        Set<String> visitados = new HashSet<>();

        fila.add(List.of(recursoInicial));
        visitados.add(recursoInicial);

        while (!fila.isEmpty()) {
            List<String> caminho = fila.poll();
            String atual = caminho.get(caminho.size() - 1);

            if (Objects.equals(atual, recursoAlvo)) {
                return Optional.of(caminho);
            }

            for (String vizinho : grafo.getOrDefault(atual, Set.of())) {
                if (!visitados.contains(vizinho)) {
                    visitados.add(vizinho);

                    List<String> novo = new ArrayList<>(caminho);
                    novo.add(vizinho);

                    fila.add(novo);
                }
            }
        }
        return Optional.empty();
    }}

