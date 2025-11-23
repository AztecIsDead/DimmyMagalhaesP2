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

            if (parts.length != 7) {
                System.err.println("linha inválida: " + line);
                continue;
            }

            try {
                long timestamp = Long.parseLong(parts[0]);
                String userId = parts[1];
                String sessionId = parts[2];
                String actionType = parts[3];
                String targetResource = parts[4];
                int severityLevel = Integer.parseInt(parts[5]);
                long bytesTransferred = Long.parseLong(parts[6]);

                LogEntry entry = new LogEntry(
                        timestamp,
                        userId,
                        sessionId,
                        actionType,
                        targetResource,
                        severityLevel,
                        bytesTransferred
                );

                list.add(entry);

            } catch (NumberFormatException e) {
                System.err.println("linha inválida: " + line);
            }
        }

        return list;
    }
}
