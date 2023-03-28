package ch.idsia.credici;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.Test;


public class TableTest {
    
    @Test
    public void create() throws IOException{
        File file = File.createTempFile("Table", ".csv");
        Files.writeString(file.toPath(), 
        "12 3 4 2 5 6\n"+
        "0 1 0 1 0 0\n"+
        "1 1 0 1 0 0\n"+
        "0 1 0 1 0 0\n"+
        "0 1 0 1 0 0\n"+
        "0 1 0 0 1 0\n", Charset.defaultCharset());

        Table table = Table.readTable(file.getAbsolutePath());
        System.out.println(table.toString());
        System.out.println(table.subtable(new int[]{4}));
    }
}
