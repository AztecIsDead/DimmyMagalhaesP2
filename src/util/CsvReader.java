package util;

import model.LogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CsvReader {

    public static List<LogEntry> read(Reader reader) throws IOException {
        List<LogEntry> list = new ArrayList<>();

        BufferedReader br = new BufferedReader(reader);
        String line;

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");

            if (parts.length != 3) {
                System.err.println("linha inválida: " + line);
                continue;
            }

            try {
                long timestamp = Long.parseLong(parts[0]);
                String userId = parts[1];
                int action = Integer.parseInt(parts[2]);

                LogEntry entry = new LogEntry(timestamp, userId, action);
                list.add(entry);

            } catch (NumberFormatException e) {
                System.err.println("linha inválida: " + line);
            }
        }

        return list;
    }
}
