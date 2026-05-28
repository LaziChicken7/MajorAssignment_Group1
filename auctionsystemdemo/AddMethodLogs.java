import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class AddMethodLogs {
    public static void main(String[] args) throws Exception {
        Path src = Paths.get("src/main/java/com/auction/controller");
        List<Path> files = Files.walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        // Regex tìm khai báo hàm: ví dụ "public void handleLogin(ActionEvent event) {"
        // Bao gồm cả trường hợp có @FXML ngay đằng trước hoặc trên dòng trước
        Pattern methodPattern = Pattern.compile("^[ \\t]*(?:@FXML\\s+)?(?:public|private|protected)\\s+(?:[\\w<>\\[\\],.?]+\\s+)+(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[^{]+)?\\s*\\{", Pattern.MULTILINE);

        for (Path p : files) {
            String content = new String(Files.readAllBytes(p));
            boolean changed = false;
            
            Matcher m = methodPattern.matcher(content);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String fullMatch = m.group(0);
                String methodName = m.group(1);
                
                // Chỉ thêm log cho các hàm xử lý sự kiện hoặc khởi tạo phổ biến để tránh làm nhiễu log với các hàm getter/setter
                if (methodName.equals("initialize") || methodName.startsWith("handle") || methodName.startsWith("load") || methodName.startsWith("show") || methodName.startsWith("open") || methodName.startsWith("go")) {
                    
                    // Nếu hàm này chưa có dòng log.info("Execute: methodName") thì mới thêm
                    String logStmt = "\n        log.info(\"\\u25B6 Controller Action - Execute: " + methodName + "()\");";
                    if (!content.substring(m.end()).trim().startsWith("log.info(\"▶ Controller Action")) {
                        m.appendReplacement(sb, Matcher.quoteReplacement(fullMatch + logStmt));
                        changed = true;
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
                    }
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(fullMatch));
                }
            }
            m.appendTail(sb);
            
            if (changed) {
                // Ensure @Slf4j exists just in case (already did this but safety first)
                String newContent = sb.toString();
                if (!newContent.contains("@Slf4j")) {
                    newContent = newContent.replaceFirst("(?m)^public class", "@lombok.extern.slf4j.Slf4j\npublic class");
                }
                Files.write(p, newContent.getBytes());
                System.out.println("Added method execution logs to " + p.getFileName());
            }
        }
    }
}
