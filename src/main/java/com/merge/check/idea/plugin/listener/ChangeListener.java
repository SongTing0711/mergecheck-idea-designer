package com.merge.check.idea.plugin.listener;

import java.util.List;

import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.project.ProjectManager;
import com.merge.check.idea.plugin.handler.MissingHandler;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import com.merge.check.idea.plugin.cache.CacheUtil;
import com.merge.check.idea.plugin.common.CommonUtil;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class ChangeListener implements BulkFileListener {
    private static final Logger log = Logger.getInstance(ChangeListener.class);

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        try {
            fileChange(events);
        } catch (Exception e) {
            log.error(" merge-check BulkFileListener error", e.getMessage());
        }

    }

    private static void fileChange(List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null) {
                continue;
            }
            if (!CommonUtil.JAVA.equals(file.getExtension())) {
                continue;
            }
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            log.info(project.getName() + " merge-check change file " + file.getName());
            // 获取变更列表管理器
            ChangeListManager changeListManager = ChangeListManager.getInstance(project);
            Change change = changeListManager.getChange(file);
            if (change == null) {
                log.info(project.getName() + " merge-check change is null");
                continue;
            }
            if (change.getType() != Change.Type.MODIFICATION) {
                log.info(project.getName() + " merge-check change type is not MODIFICATION");
                continue;
            }
            // 获取变更文件的提交信息
            ContentRevision beforeRevision = change.getBeforeRevision();
            ContentRevision afterRevision = change.getAfterRevision();
            // 获取变更前的代码内容
            String afterContent = null;
            if (afterRevision != null) {
                try {
                    afterContent = afterRevision.getContent();
                } catch (VcsException e) {
                    log.error(project.getName() + " merge-check get after content error", e.getMessage());
                    continue;
                }
            }
            if (afterContent == null) {
                log.info(project.getName() + " merge-check after content is null");
                continue;
            }
            String conflictKey = project.getName() + file.getPath() + CommonUtil.CONFLICT;
            String conflictContentCache = CacheUtil.PROJECT_CONFLICT_CACHE.getIfPresent(conflictKey);
            if (conflictContentCache == null) {
                log.info(project.getName() + " merge-check conflictContentCache is null");
                continue;
            }
            String beforeContent = null;
            if (beforeRevision != null) {
                try {
                    beforeContent = beforeRevision.getContent();
                } catch (VcsException e) {
                    log.error(project.getName() + " merge-check get before content error", e.getMessage());
                    continue;
                }
            }
            if (beforeContent == null) {
                log.info(project.getName() + " merge-check before content is null");
                continue;
            }
            if (!afterContent.contains(CommonUtil.CONFLICT_MARK)
                && !afterContent.contains(CommonUtil.CONFLICT_MARK_END)) {
                log.info(project.getName() + " merge-check after content not contains conflict mark");

                log.info(project.getName() + " merge-check after content");
                CacheUtil.PROJECT_CONFLICT_CACHE.invalidate(conflictContentCache);
                MissingHandler.findMissingLines(project, file.getName(), beforeContent, conflictContentCache,
                    afterContent);
            }

        }
    }

}
