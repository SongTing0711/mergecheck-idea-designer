package com.merge.check.idea.plugin.common;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @Author : song hao
 * @CreateTime : 2024/08/02 16:17
 * @Description :
 */

public class CommonUtil {
    public static final String CONFLICT_MARK = "<<<<<<<";

    public static final String CONFLICT_MARK_END = ">>>>>>>";

    public static final String CONFLICT = "CONFLICT";

    public static final String LINE_BREAK = "\n";
    public static final String JAVA = "java";

    public static final String COMMON_LINE_BREAK = "\\r?\\n";
    public static final String IMPORT = "import";
    public static final String AUTOWIRED = "@Autowired";
    public static final String INJECT = "@Inject";
    public static final String RESOURCE = "@Resource";

    public static Set<String> processContent(String content) {
        if (StringUtils.isBlank(content)) {
            return ConcurrentHashMap.newKeySet();
        }
        return Arrays.stream(content.split(COMMON_LINE_BREAK)).map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith(IMPORT) && !line.contains(AUTOWIRED)
                && !line.contains(INJECT) && !line.contains(RESOURCE))
            .collect(Collectors.toSet());
    }

    public static List<String> processContentToList(String content) {
        if (StringUtils.isBlank(content)) {
            return new ArrayList<>();
        }
        return Arrays.stream(content.split(COMMON_LINE_BREAK)).map(String::trim)
            .filter(line -> !line.isEmpty() && !line.startsWith(IMPORT) && !line.contains(AUTOWIRED)
                && !line.contains(INJECT) && !line.contains(RESOURCE))
            .collect(Collectors.toList());
    }
}
