package tests;

import com.adamcalculator.dynamicpack.pack.dynamicrepo.DynamicRepoSyncBuilder;
import com.adamcalculator.dynamicpack.util.Out;
import com.adamcalculator.dynamicpack.util.Urls;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SecurityTrustedUrlsTest {

    @Test
    public void d() {
        Out.USE_SOUT = true;

        Assertions.assertThrows(Exception.class, () -> Urls.parseTextContent("https://google.com", 1732132132));
        Assertions.assertThrows(Exception.class, () -> Out.println(Urls.parseTextContent("https://modrinth.com", 6)));

        Assertions.assertThrows(Exception.class, () -> Urls.parseTextContent("https://modrinth.com.google.com", 1732132132));

        Assertions.assertThrows(Exception.class, () -> Urls.parseTextContent("https://fakemodrinth.com.com", 1732132132));


        Assertions.assertDoesNotThrow(() -> {
            DynamicRepoSyncBuilder.getAndCheckPath("assets", "minecraft/lang/en_us.json");
            DynamicRepoSyncBuilder.getAndCheckPath("assets/", "minecraft/lang/en_us.json");
            DynamicRepoSyncBuilder.getAndCheckPath("assets", "/minecraft/lang/en_us.json");
            DynamicRepoSyncBuilder.getAndCheckPath("assets", "/minecraft/lang/en_us.json");
            DynamicRepoSyncBuilder.getAndCheckPath("/assets/", "///minecraft/lang/en_us.json");
        });



        Assertions.assertThrows(Exception.class, () -> {
            DynamicRepoSyncBuilder.getAndCheckPath("assets/../../../../", "minecraft/lang/en_us.json");
        });

        Assertions.assertThrows(Exception.class, () -> {
            DynamicRepoSyncBuilder.getAndCheckPath("assets/../../../../", "minecraft/lang/en_us.json");
        });

        Assertions.assertThrows(Exception.class, () -> {
            DynamicRepoSyncBuilder.getAndCheckPath("assets/../../../../", "minecraft/lang/en_us.json");
        });

    }
}
