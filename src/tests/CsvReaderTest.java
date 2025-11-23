package tests;

import model.LogEntry;
import util.CsvReader;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvReaderTest {

    @Test
    public void testCsvReaderSimple() throws Exception {
        String csv = ""
                + "1700000000000,userA,session1,LOGIN,/bin/bash,3,10\n"
                + "1700000001000,userB,session2,EXEC,/usr/bin/ls,5,1024\n"
                + "invalid_line_here";

        List<LogEntry> list = CsvReader.read(new StringReader(csv));

        assertEquals(2, list.size());
    }

}
