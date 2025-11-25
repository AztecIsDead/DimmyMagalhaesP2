package service;

import br.edu.icev.aed.forense.Alerta;
import br.edu.icev.aed.forense.AnaliseForenseAvancada;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ResolverDesafio implements AnaliseForenseAvancada {
    //metodo auxiliar para lidar com CSV
    private static int proximaVirgula( String string, int inicio){
        //encontra a proxima virgula no arquivo CSV para otimizar a leitura
        //busca a partir do indice 'inicio'
        return string.indexOf(',', inicio);
    }

    //segundo metodo auxiliar
    //consegue ler N campos em um array, com tolerancia a linhas curtas
    public static boolean parseLinha(String linha, String[] campos){
        int inicio = 0;
        int posicao;

        for (int i = 0; i <campos.length; i++) {
            posicao = proximaVirgula(linha, inicio);
            if (posicao < 0){
                return false; //detecta linha invalida
            }
            campos[i] = linha.substring(inicio, posicao);
            inicio = posicao + 1;
        }

        campos[campos.length - 1] = linha.substring(inicio);
        return true;
    }

    @Override
    public Set<String> encontrarSessoesInvalidas(String caminhoArquivo) throws IOException {
        enum Acao {LOGIN, LOGOUT} //uso de 'enums' para reduzir custo de parsing

        //cada usuario possui uma pilha de sessoes (modelo LIFO)
        //usada para detectar logins aninhados e logouts incorretos
        Map<String, Deque<String>> pilhasPorUsuario = new HashMap<>();

        //armazena quaisquer sessoes que apresentam alguma inconsistência
        Set<String> sessoesInvalidas = new HashSet<>();

        //campos CSV reutilizados para evitar alocacao repetida
        String[] campos = new String[7];

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            while ((linha = reader.readLine()) != null) {
                if (!parseLinha(linha, campos)) {
                    continue; //linha inadequada
                }

                String userId = campos[1];
                String sessionId = campos[2];
                String actionText = campos[3];

                Acao acao;
                try {
                    acao = Acao.valueOf(actionText);
                } catch (IllegalArgumentException e) {
                    continue; // acao invalida
                }

                Deque<String> pilha = pilhasPorUsuario.computeIfAbsent(userId, k -> new ArrayDeque<>());

                switch (acao) {

                    case LOGIN -> {
                        //LOGIN enquanto ha sessao ativa significa que e invalido
                        if (!pilha.isEmpty()) {
                            sessoesInvalidas.add(sessionId);
                        }
                        pilha.push(sessionId);
                    }

                    case LOGOUT -> {
                        //LOGOUT sem LOGIN
                        if (pilha.isEmpty()) {
                            sessoesInvalidas.add(sessionId);
                        //LOGOUT errado
                        } else if (!Objects.equals(pilha.peek(), sessionId)) {
                            sessoesInvalidas.add(sessionId);
                        //LOGOUT correto
                        } else {
                            pilha.pop();
                        }
                    }
                }
            }
        }

        // sessoes que ficaram na pilha nunca tiveram logout
        for (Deque<String> p : pilhasPorUsuario.values()) {
            sessoesInvalidas.addAll(p);
        }

        return sessoesInvalidas;
    }

    @Override
    public List<String> reconstruirLinhaTempo(String caminhoArquivo, String sessionId) throws IOException {
        //FIFO para manter cronologia
        Queue<String> fila = new ArrayDeque<>();
        String[] campos = new String[7];

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;
            while ((linha = reader.readLine()) != null) {

                if (!parseLinha(linha, campos)) continue;

                if (sessionId.equals(campos[2])) {
                    fila.add(campos[3]); // ACTION_TYPE
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
        // caso trivial
        if (n <= 0) return Collections.emptyList();

        //PriorityQueue em ordem decrescente de severidade
        PriorityQueue<Alerta> filaPrioridade = new PriorityQueue<>(
                (a1, a2) -> Integer.compare(a2.getSeverityLevel(), a1.getSeverityLevel())
        );

        String[] campos = new String[7];

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;
            while ((linha = reader.readLine()) != null) {

                if (!parseLinha(linha, campos)) continue;

                try {
                    Alerta alerta = new Alerta(
                            Long.parseLong(campos[0]),
                            campos[1],
                            campos[2],
                            campos[3],
                            campos[4],
                            Integer.parseInt(campos[5]),
                            Long.parseLong(campos[6])
                    );
                    filaPrioridade.add(alerta);

                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        //retira os N maiores alertas por severidade
        List<Alerta> alertas = new ArrayList<>(n);
        for (int i = 0; i < n && !filaPrioridade.isEmpty(); i++) {
            alertas.add(filaPrioridade.poll());
        }

        return alertas;
    }

    @Override
    public Map<Long, Long> encontrarPicosTransferencia(String caminhoArquivo) throws IOException {
        List<long[]> eventos = new ArrayList<>();
        String[] campos = new String[7];

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;
            while ((linha = reader.readLine()) != null) {

                if (!parseLinha(linha, campos)) continue;

                try {
                    long timestamp = Long.parseLong(campos[0]);
                    long bytes = Long.parseLong(campos[6]);

                    if (bytes > 0) {
                        eventos.add(new long[]{timestamp, bytes});
                    }

                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        Map<Long, Long> encontrados = new HashMap<>();
        ArrayDeque<long[]> pilha = new ArrayDeque<>();

        //percorre de tras para frente buscando primeiro maior
        for (int i = eventos.size() - 1; i >= 0; i--) {

            long ts = eventos.get(i)[0];
            long bytes = eventos.get(i)[1];

            while (!pilha.isEmpty() && pilha.peek()[1] <= bytes) {
                pilha.pop();
            }

            if (!pilha.isEmpty()) {
                encontrados.put(ts, pilha.peek()[0]);
            }

            pilha.push(new long[]{ts, bytes});
        }

        return encontrados;
    }

    @Override
    public Optional<List<String>> rastrearContaminacao(String caminhoArquivo, String recursoInicial, String recursoAlvo)
            throws IOException {

        if (recursoInicial == null || recursoAlvo == null ||
                recursoInicial.isEmpty() || recursoAlvo.isEmpty()) {
            return Optional.empty();
        }

        //grafo
        Map<String, List<String>> adjacencias = new HashMap<>();
        String[] campos = new String[7];

        try (BufferedReader reader = new BufferedReader(new FileReader(caminhoArquivo))) {

            String linha;

            String ultimaSessao = null;
            String ultimoRecurso = null;

            while ((linha = reader.readLine()) != null) {

                if (!parseLinha(linha, campos)) continue;

                String session = campos[2];
                String resource = campos[4];

                //ao mudar de sessao a cadeia de contaminacao reinicia
                if (!session.equals(ultimaSessao)) {
                    ultimaSessao = session;
                    ultimoRecurso = null;
                }

                //adiciona aresta entre recursos consecutivos na sessao
                if (ultimoRecurso != null) {
                    adjacencias.computeIfAbsent(ultimoRecurso, k -> new ArrayList<>())
                            .add(resource);
                }

                ultimoRecurso = resource;
            }
        }

        //caso especial: recursoInicial == recursoAlvo
        if (recursoInicial.equals(recursoAlvo)) {
            boolean existe = adjacencias.containsKey(recursoInicial)
                    || adjacencias.values().stream().anyMatch(l -> l.contains(recursoInicial));
            return existe ? Optional.of(List.of(recursoInicial)) : Optional.empty();
        }

        //BFS
        Queue<String> fila = new ArrayDeque<>();
        Map<String, String> predecessor = new HashMap<>();
        Set<String> visitado = new HashSet<>();

        fila.add(recursoInicial);
        visitado.add(recursoInicial);

        boolean encontrado = false;

        while (!fila.isEmpty()) {
            String atual = fila.poll();

            List<String> adj = adjacencias.get(atual);
            if (adj == null) continue;

            for (String prox : adj) {
                if (!visitado.contains(prox)) {

                    visitado.add(prox);
                    predecessor.put(prox, atual);
                    fila.add(prox);

                    if (prox.equals(recursoAlvo)) {
                        encontrado = true;
                        break;
                    }
                }
            }
            if (encontrado) break;
        }

        if (!encontrado) {
            return Optional.empty();
        }

        //reconstruir caminho
        List<String> caminho = new ArrayList<>();
        String cur = recursoAlvo;

        while (cur != null) {
            caminho.add(cur);
            cur = predecessor.get(cur);
        }

        Collections.reverse(caminho);
        return Optional.of(caminho);
    }
}