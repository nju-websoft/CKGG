package loaders;

import java.io.IOException;

import test.BaseLoad;
import test.CountryLoader;
import test.TzLoader;

public class LoadGeonamesTriples {
    /**
     * 
     * @param outputPath TODO!!!
     * @throws IOException
     */
    public void runmain(String outputPath) throws IOException {
        AlternateNameLoad.main(null);
        BaseLoad.main(null);
        LoadAdm.main(null);
        CountryLoader.main(null);
        TzLoader.main(null);
    }
}
