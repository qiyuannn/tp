package trackflow.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class ApplicationHomeTest {

    @Test
    public void getPath_unpackedClasses_usesWorkingDirectory() throws Exception {
        // EP: application home in unpacked classes environment resolves to working directory
        Path classes = Path.of(ApplicationHome.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isDirectory(classes));
        assertEquals(Path.of("").toAbsolutePath().normalize(), ApplicationHome.getPath()); // Boundary value

        // EP: valid relative path with parent navigation staying within application home
        assertEquals(ApplicationHome.getPath().resolve("preferences.json"),
                ApplicationHome.resolve("data/../preferences.json"));
    }

    @Test
    public void resolve_pathOutsideHome_throwsIllegalArgumentException() {
        // EP: relative path escaping application home via parent directory navigation
        assertThrows(IllegalArgumentException.class, () ->
                ApplicationHome.resolve("../outside.json")); // Boundary value

        // EP: absolute path located outside application home
        String absoluteOutside = ApplicationHome.getPath().getParent().resolve("outside.json").toString();
        assertThrows(IllegalArgumentException.class, () -> ApplicationHome.resolve(absoluteOutside));
    }
}
