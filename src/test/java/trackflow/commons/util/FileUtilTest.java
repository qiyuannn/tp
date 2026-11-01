package trackflow.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilTest {
    @TempDir
    public Path temporaryFolder;

    // EP: existing destination file (overwrites content and cleans temporary file)
    @Test
    public void writeToFile_existingFile_replacesContentAndCleansTemporaryFile() throws Exception {
        Path target = temporaryFolder.resolve("roster.json");
        Files.writeString(target, "previous roster");

        FileUtil.writeToFile(target, "updated roster");

        assertEquals("updated roster", Files.readString(target));
        assertEquals(1, countFilesIn(temporaryFolder));
    }

    // EP: file content with leading byte order mark (BOM)
    @Test
    public void readFromFile_stripsByteOrderMark() throws Exception {
        Path target = temporaryFolder.resolve("roster.json");
        Files.writeString(target, "\uFEFF{ \"persons\" : [ ] }");

        assertEquals("{ \"persons\" : [ ] }", FileUtil.readFromFile(target));
    }

    // EP: file content without byte order mark (BOM)
    @Test
    public void readFromFile_noByteOrderMark_returnsContentUnchanged() throws Exception {
        Path target = temporaryFolder.resolve("roster.json");
        Files.writeString(target, "{ \"persons\" : [ ] }");

        assertEquals("{ \"persons\" : [ ] }", FileUtil.readFromFile(target));
    }

    // EP: destination path with non-existent parent directories (creates directories and file)
    @Test
    public void writeToFile_missingParents_createsFile() throws Exception {
        Path target = temporaryFolder.resolve("nested/data/roster.json");

        FileUtil.writeToFile(target, "roster");

        assertEquals("roster", Files.readString(target));
        assertEquals(1, countFilesIn(target.getParent()));
    }

    // EP: destination path is existing directory (replacement fails, preserves destination and cleans temporary file)
    @Test
    public void writeToFile_replacementFails_preservesDestinationAndCleansTemporaryFile() throws Exception {
        Path target = Files.createDirectory(temporaryFolder.resolve("roster.json"));
        Path original = target.resolve("original");
        Files.writeString(original, "preserved");

        assertThrows(IOException.class, () -> FileUtil.writeToFile(target, "replacement"));

        assertEquals("preserved", Files.readString(original));
        assertEquals(1, countFilesIn(temporaryFolder));
    }

    @Test
    public void readFromFile_existingAndMissingFile() throws Exception {
        // EP: existing readable file
        Path file = temporaryFolder.resolve("sample.txt");
        Files.writeString(file, "hello trackflow");
        assertEquals("hello trackflow", FileUtil.readFromFile(file));

        // EP: non-existent file
        Path missing = temporaryFolder.resolve("missing.txt");
        assertThrows(IOException.class, () -> FileUtil.readFromFile(missing));
    }

    private static long countFilesIn(Path folder) throws IOException {
        try (var files = Files.list(folder)) {
            return files.count();
        }
    }
}
