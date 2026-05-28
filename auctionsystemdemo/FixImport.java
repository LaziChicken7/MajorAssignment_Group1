import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class FixImport {
    public static void main(String[] args) throws Exception {
        Path src = Paths.get("src/main/java");
        List<Path> files = Files.walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        for (Path p : files) {
            String content = new String(Files.readAllBytes(p));
            if (content.contains("@Slf4j") && !content.contains("import lombok.extern.slf4j.Slf4j;")) {
                content = content.replaceFirst("(?m)^package\\s+[^;]+;\\s*", "$0\nimport lombok.extern.slf4j.Slf4j;\n");
                Files.write(p, content.getBytes());
                System.out.println("Fixed import for " + p);
            }
        }
    }
}
