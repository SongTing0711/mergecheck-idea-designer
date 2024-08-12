package com.merge.check.idea.plugin.listener;

import java.util.List;

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
public class ChangeListener {
    private static final Logger log = Logger.getInstance(ChangeListener.class);

    public static void handle(Project project) {

        MessageBusConnection connection = project.getMessageBus().connect();
        // 获取变更列表管理器
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                try {
                    fileChange(project, events, changeListManager);
                } catch (Exception e) {
                    log.error(project.getName() + " merge-check BulkFileListener error", e.getMessage());
                }

            }
        });
    }

    private static void fileChange(Project project, List<? extends VFileEvent> events,
        ChangeListManager changeListManager) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null) {
                continue;
            }
            if (!CommonUtil.JAVA.equals(file.getExtension())) {
                continue;
            }
            log.info(project.getName() + " merge-check change file " + file.getName());
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
