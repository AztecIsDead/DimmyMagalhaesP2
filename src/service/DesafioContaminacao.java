package service;

import model.LogEntry;

import java.util.*;

public class DesafioContaminacao {
    public List<String> rastrearContaminacao(List<LogEntry> logs,
                                             String origem,
                                             String destino) {
        Map<String, List<LogEntry>> porSessao = new HashMap<>();
        for (LogEntry l : logs) {
            porSessao.computeIfAbsent(l.getSessionId(), k -> new ArrayList<>()).add(l);
        }
        Map<String, Set<String>> grafo = new HashMap<>();
        for (List<LogEntry> sessao : porSessao.values()) {
            sessao.sort(Comparator.comparingLong(LogEntry::getTimestamp));
            for (int i = 0; i < sessao.size() - 1; i++) {
                String r1 = sessao.get(i).getTargetResource();
                String r2 = sessao.get(i + 1).getTargetResource();

                grafo.computeIfAbsent(r1, k -> new HashSet<>()).add(r2);
                grafo.computeIfAbsent(r2, k -> new HashSet<>()).add(r1);
            }
        }
        Queue<String> fila = new LinkedList<>();
        Map<String, String> pai = new HashMap<>();
        fila.add(origem);
        pai.put(origem, null);

        while (!fila.isEmpty()) {
            String atual = fila.poll();

            if (atual.equals(destino)) break;

            for (String viz : grafo.getOrDefault(atual, Set.of())) {
                if (!pai.containsKey(viz)) {
                    pai.put(viz, atual);
                    fila.add(viz);
                }
            }
        }
        if (!pai.containsKey(destino)) {
            return List.of();
        }
        List<String> caminho = new ArrayList<>();
        String cur = destino;
        while (cur != null) {
            caminho.add(cur);
            cur = pai.get(cur);
        }
        Collections.reverse(caminho);
        return caminho;
    }
}