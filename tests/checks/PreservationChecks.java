package checks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** SHA-256 of the original scheduling/model regions, normalized only for line endings. */
public final class PreservationChecks {
    public static void main(String[] args) throws Exception {
        int count = 0;
        for (String row : Files.readAllLines(Path.of("tests/algorithm-baselines.tsv"))) {
            if (row.isBlank() || row.startsWith("#")) continue;
            String[] columns = row.split("\t");
            String source = Files.readString(Path.of(columns[0])).replace("\r\n", "\n");
            int start = source.indexOf(columns[1]);
            int end = source.indexOf(columns[2], start);
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(source.substring(start, end).getBytes(StandardCharsets.UTF_8)));
            if (!hash.equals(columns[3])) throw new AssertionError("Algorithm region changed: " + columns[0]);
            count++;
        }
        if (count != 5) throw new AssertionError("Missing algorithm baselines");
        System.out.println("PASS preservation: all 5 original scheduling/model regions unchanged");
    }
}
