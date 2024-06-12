package tests;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.Urls;
import org.junit.jupiter.api.Test;

public class UrlsTest {
    @Test
    public void valid() throws Exception {
        Out.USE_SOUT = true;
        DynamicPackMod.addAllowedHosts("ubuntu.com", this);
        int i = 0;
        while (i < 100) {
            Urls.downloadFileToTemp("https://cdn.modrinth.com/data/UQBo9Yss/versions/fYk7cLrO/BetterTables.zip", "test", ".temp", 32965550080L, null);
            i++;
        }
    }
}
