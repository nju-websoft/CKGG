package load;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import namespaces.Namespaces;

public class AuxLoader {
    private static final Logger logger = LoggerFactory.getLogger(AuxLoader.class);
    Map<String, String> m;

    public AuxLoader(String path) throws IOException {
        m = new HashMap<>();
        ImmutableList<String> lines = Files.asCharSource(new File(path), StandardCharsets.UTF_8).readLines();

        int cur = 1;
        for(String line: lines) {
            m.put(line.strip(), Namespaces.GKDA + "I" + (cur++));
        }

        logger.info("aux loader size {}", m.size());
    }

    public String loadAuxUri(String x) {
        if(!m.containsKey(x)) {
            throw new IndexOutOfBoundsException();
        }
        return m.get(x);
    }
}