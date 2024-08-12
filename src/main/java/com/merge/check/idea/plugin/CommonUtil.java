package com.merge.check.idea.plugin;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author : song hao
 * @CreateTime : 2024/08/02 16:17
 * @Description :
 */

public class CommonUtil {
    public static final ConcurrentHashMap<String, String> conflictContentMap = new ConcurrentHashMap<>();
    public static final String CONFLICT_MARK = "<<<<<<<";

    public static final String CONFLICT_MARK_END = ">>>>>>>";

    public static final String CONFLICT = "CONFLICT";

    public static final String LINE_BREAK = "\n";
    public static final String JAVA = "java";
}
