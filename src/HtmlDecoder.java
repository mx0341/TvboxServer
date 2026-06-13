import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlDecoder {
    // 预定义常见 HTML 命名实体的映射表
    private static final Map<String, String> NAMED_ENTITIES = new HashMap<>();
    static {
        NAMED_ENTITIES.put("lt", "<");
        NAMED_ENTITIES.put("gt", ">");
        NAMED_ENTITIES.put("amp", "&");
        NAMED_ENTITIES.put("quot", "\"");
        NAMED_ENTITIES.put("apos", "'");
        NAMED_ENTITIES.put("nbsp", " ");
        // 可以根据需要继续添加更多实体
    }
    /**
     * 将 HTML 字符串解码为普通文本
     * 包括十进制数值实体 (&#xxx;)、十六进制数值实体 (&#x00xx;) 和命名实体 (&entity;)
     *
     * @param htmlContent HTML 字符串内容
     * @return 解码后的普通文本字符串
     */
    public static String htmlDecode(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }
        // 处理数字实体（十进制和十六进制）
        String result = processNumericEntities(htmlContent);
        // 处理命名实体（如 &lt;、&amp; 等）
        return processNamedEntities(result);
    }
	
    /**
     * 处理 HTML 中的数值实体
     */
    private static String processNumericEntities(String input) {
        Pattern pattern = Pattern.compile("&(?:#x([0-9a-fA-F]+)|#([0-9]+));", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                String hexGroup = matcher.group(1);
                String decGroup = matcher.group(2);
                int codePoint;
                if (hexGroup != null) {
                    codePoint = Integer.parseInt(hexGroup, 16);
                } else {
                    codePoint = Integer.parseInt(decGroup);
                }
                // 处理代理字符范围的 Unicode
                if (codePoint > Character.MAX_CODE_POINT) {
                    codePoint = 0xFFFD; // 替换为 "?"
                }
                char[] chars = Character.toChars(codePoint);
                matcher.appendReplacement(buffer, new String(chars));
            } catch (NumberFormatException e) {
                // 如果解析异常，保留在其中
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
	
    /**
     * 处理 HTML 中的命名实体（如 &lt;、&amp; 等）
     */
    private static String processNamedEntities(String input) {
        Pattern pattern = Pattern.compile("&([^;]+);", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String entityName = matcher.group(1);
            String replacement = NAMED_ENTITIES.getOrDefault(entityName, null);
            if (replacement != null) {
                matcher.appendReplacement(buffer, replacement);
            } else {
                // 不识别的实体保留原样
				//System.out.println(matcher.group(0));
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
