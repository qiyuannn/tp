package trackflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trackflow.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import trackflow.commons.util.FileUtil;
import trackflow.model.person.Person;
import trackflow.storage.JsonRosterStorage;
import trackflow.testutil.PersonBuilder;
import trackflow.testutil.ProcessTestUtil;

/**
 * Runs startup in child JVMs so application-home and logging state cannot leak between scenarios.
 * The temporary JAR contains application classes and uses the test runtime dependencies; it does not open JavaFX
 * windows or establish that the final release JAR works on every supported platform.
 */
public class StartupProcessTest {
    @TempDir
    public Path testFolder;

    @Test
    public void startup_jarInAnotherDirectory_keepsRecoveryAndSavedFilesInJarHome() throws Exception {
        Path home = Files.createDirectory(testFolder.resolve("JAR home %u with spaces")).toRealPath();
        Path launch = Files.createDirectory(testFolder.resolve("launch"));
        Path jar = createApplicationJar(home);
        Path rosterPath = home.resolve("data/trackflow.json");
        Files.createDirectories(rosterPath.getParent());
        // EP: child JVM execution with corrupted JSON recovering in JAR home directory
        String original = "{\"persons\":[]} {\"persons\":[{}]}";
        Files.writeString(rosterPath, original); // Boundary value
        String output = ProcessTestUtil.runJava(jar, launch, testFolder.resolve("first-output.txt"),
                StartupProcessProbe.class, "change");
        assertTrue(output.contains("ROSTER_FILE=" + rosterPath), output);
        Person expected = new PersonBuilder().withName("Avery Tan").withAgeCategory("Under 14")
                .withPhone("91234567").withEmail("avery@example.com").withAddress("23 Bishan Street")
                .withRemark("Weekend training").withTags("100m").build();
        assertEquals(List.of(expected), new JsonRosterStorage(rosterPath).readRoster().orElseThrow().getPersonList());
        assertTrue(Files.exists(home.resolve("preferences.json")));
        assertTrue(Files.exists(home.resolve("trackflow.log.0")));
        assertTrue(Files.readString(home.resolve("trackflow.log.0")).contains("STARTUP_PROCESS_OK"));
        // EP: backup file created for unreadable roster in JAR home directory
        try (Stream<Path> paths = Files.list(rosterPath.getParent())) {
            List<Path> backups = paths.filter(p -> p.getFileName().toString().endsWith(".bak")).toList();
            assertEquals(1, backups.size()); // Boundary value
            assertEquals(original, Files.readString(backups.getFirst()));
        }
        // EP: second launch preserves previously saved data without creating files in launch directory
        String saved = Files.readString(rosterPath);
        ProcessTestUtil.runJava(jar, launch, testFolder.resolve("reopen-output.txt"), StartupProcessProbe.class);
        assertEquals(saved, Files.readString(rosterPath));
        try (Stream<Path> paths = Files.list(launch)) {
            assertEquals(0, paths.count(), "The launch directory must not receive application files"); // Boundary value
        }
        // EP: cleanup of temporary sibling files on successful and failed operations
        try (Stream<Path> paths = Files.walk(home)) {
            assertFalse(paths.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
        // A failed replacement cleans up its temporary sibling inside the same home directory.
        Path blocked = Files.createDirectory(home.resolve("data/blocked.json"));
        Files.writeString(blocked.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> FileUtil.writeToFile(blocked, "new"));
        assertEquals("keep", Files.readString(blocked.resolve("keep.txt")));
        try (Stream<Path> paths = Files.list(blocked.getParent())) {
            assertFalse(paths.anyMatch(p -> p.getFileName().toString().endsWith(".tmp")));
        }
    }

    // EP: startup when log file cannot be opened continues execution and preserves roster
    @Test
    public void startup_fileLoggerCannotOpen_continuesWithoutChangingRoster() throws Exception {
        Path home = Files.createDirectory(testFolder.resolve("logging failure"));
        Path jar = createApplicationJar(home);
        Path roster = home.resolve("data/trackflow.json");
        Files.createDirectories(roster.getParent());
        String saved = "{\"persons\":[]}"; // Boundary value
        Files.writeString(roster, saved);
        // A directory at the first rotated log path reliably prevents opening it on every OS.
        Files.createDirectory(home.resolve("trackflow.log.0"));
        String output = ProcessTestUtil.runJava(jar, home, testFolder.resolve("logging-output.txt"),
                StartupProcessProbe.class);
        assertTrue(output.contains("STARTUP_PROCESS_OK"), output);
        assertTrue(output.contains("File logging unavailable"), output);
        assertFalse(output.contains("ExceptionInInitializerError"), output);
        assertEquals(saved, Files.readString(roster));
    }

    private Path createApplicationJar(Path home) throws Exception {
        Path classes = Path.of(MainApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path jar = home.resolve("TrackFlow.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
                Stream<Path> paths = Files.walk(classes)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                output.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, output);
                output.closeEntry();
            }
        }
        return jar;
    }
}
