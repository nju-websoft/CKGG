package load;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ext.com.google.common.io.Files;

public class GetAux {
    public static void main(String[] args) throws IOException {
        Set<String> tzlines = new HashSet<>();
        Files.asCharSource(new File("/home/ylshen/elinga/geonames/timeZones.txt"), StandardCharsets.UTF_8).lines().skip(1).forEach(
            line -> {
                String[] resLine = line.split("\t");
                tzlines.add(resLine[1].strip());
            }
        );
        Files.asCharSink(new File("auxiliary.txt"), StandardCharsets.UTF_8).writeLines(tzlines);
    }
}
