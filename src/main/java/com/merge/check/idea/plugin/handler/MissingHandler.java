package com.merge.check.idea.plugin.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.merge.check.idea.plugin.common.CommonUtil;
import com.merge.check.idea.plugin.dialog.MergeConflictDialog;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class MissingHandler {
    private static final Logger log = Logger.getInstance(MissingHandler.class);


    public static void findMissingLines(Project project, String file, String beforeContent, String afterContent,
        String nowContent) {
        log.info(file + " merge-check findMissingLines");
        // 分割成行并去除首尾空白
        Set<String> beforeLines = CommonUtil.processContent(beforeContent);
        List<String> afterLines = CommonUtil.processContentToList(afterContent);
        Set<String> nowLines = CommonUtil.processContent(nowContent);

        List<String> remoteMissingLines = new ArrayList<>();
        List<String> localMissingLines = new ArrayList<>();
        boolean inConflictBlock = false;

        for (String line : afterLines) {
            if (line.startsWith(CommonUtil.CONFLICT_MARK)) {
                inConflictBlock = true;
            } else if (line.startsWith(CommonUtil.CONFLICT_MARK_END)) {
                inConflictBlock = false;
                continue;
            }

            if (!inConflictBlock && !nowLines.contains(line)) {
                log.info(file + " merge-check findMissingLines line:" + line);
                if (beforeLines.contains(line)) {
                    localMissingLines.add(line);
                } else {
                    remoteMissingLines.add(line);
                }
            }
        }
        String remoteText = String.join(CommonUtil.LINE_BREAK, remoteMissingLines);
        String localText = String.join(CommonUtil.LINE_BREAK, localMissingLines);
        log.info(file + " merge-check remoteMissingLines:" + remoteText);
        log.info(file + " merge-check localMissingLines:" + localText);

        if (!remoteMissingLines.isEmpty() || !localMissingLines.isEmpty()) {
            log.info(file + " merge-check show dialog");
            ApplicationManager.getApplication().invokeLater(() -> {
                MergeConflictDialog dialog = new MergeConflictDialog(file, project, remoteText, localText);
                dialog.show();
            });
        }
    }

}
