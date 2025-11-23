package service;

import model.LogEntry;

import java.util.*;

public class DesafioPicos {

    public Map<LogEntry, LogEntry> encontrarPicos(List<LogEntry> logs) {

        List<LogEntry> eventos = new ArrayList<>();
        for (LogEntry l : logs) {
            if (l.getBytesTransferred() > 0) {
                eventos.add(l);
            }
        }
        Map<LogEntry, LogEntry> resultado = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < eventos.size(); i++) {
            long atual = eventos.get(i).getBytesTransferred();

            while (!stack.isEmpty() &&
                    atual > eventos.get(stack.peek()).getBytesTransferred()) {

                int idx = stack.pop();
                resultado.put(eventos.get(idx), eventos.get(i));
            }
            stack.push(i);
        }
        while (!stack.isEmpty()) {
            resultado.put(eventos.get(stack.pop()), null);
        }
        return resultado;
    }
}
