package com.merge.check.idea.plugin.listener;

import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.merge.check.idea.plugin.cache.CacheUtil;
import com.merge.check.idea.plugin.common.CommonUtil;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class ConflictListener implements VirtualFileListener {
    private static final Logger log = Logger.getInstance(ConflictListener.class);

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        if (file == null) {
            return;
        }
        log.info(" merge-check contentsChanged: " + file.getPath());
        if (!CommonUtil.JAVA.equals(file.getExtension())) {
            return;
        }
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        String key = project.getName() + file.getPath() + CommonUtil.CONFLICT;
        if (CacheUtil.AVOID_REPEAT_CONFLICT_CACHE.getIfPresent(key) != null) {
            log.info("merge-check AVOID_REPEAT_CONFLICT_CACHE: " + file.getPath());
            return;
        }
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        log.info("merge-check document: " + file.getPath());
        String newContent = document.getText();
        if (newContent.contains(CommonUtil.CONFLICT_MARK) && newContent.contains(CommonUtil.CONFLICT_MARK_END)) {
            CacheUtil.AVOID_REPEAT_CONFLICT_CACHE.put(key, newContent);
            log.info(project.getName() + " merge-check after newContent contains conflict mark: " + file.getPath());
            CacheUtil.PROJECT_CONFLICT_CACHE.put(key, newContent);
        }

    }

}
