package com.merge.check.idea;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */

import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.merge.check.idea.plugin.CacheUtil;
import com.merge.check.idea.plugin.CommonUtil;
import com.merge.check.idea.plugin.dialog.MergeConflictDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import git4idea.GitVcs;
import git4idea.commands.GitHandler;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class MergeConflictFilesListener implements ProjectManagerListener {
    private static final Logger log = Logger.getInstance(MergeConflictFilesListener.class);

    @Override
    public void projectClosed(@NotNull Project project) {
        log.info(project.getName() + " merge-check projectClosed");
        try {
            MessageBusConnection connection = project.getMessageBus().connect();
            connection.disconnect();
        } catch (Exception e) {
            log.error(project.getName() + " merge-check projectClosed error", e.getMessage());
        }
    }

    @Override
    public void projectOpened(@NotNull Project project) {
        log.info(project.getName() + " merge-check projectOpened");
        try {
            handle(project);
        } catch (Exception e) {
            log.error(project.getName() + " merge-check projectOpened error", e.getMessage());
        }

    }

    private void handle(Project project) {

        MessageBusConnection connection = project.getMessageBus().connect();
        // 获取变更列表管理器
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        connection.subscribe(GitRepository.GIT_REPO_CHANGE, new GitRepositoryChangeListener() {
            @Override
            public void repositoryChanged(@NotNull GitRepository repository) {
                log.info(project.getName() + " merge-check repositoryChanged " + repository.getCurrentBranchName());
            }
        });
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

    private void fileChange(Project project, List<? extends VFileEvent> events, ChangeListManager changeListManager) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file == null) {
                continue;
            }
            if (!"java".equals(file.getExtension())) {
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
            if (afterContent.contains(CommonUtil.CONFLICT_MARK)) {
                log.info(project.getName() + " merge-check after content:" + afterContent);
                log.info(project.getName() + " merge-check after content contains conflict mark");
                cacheConflict(beforeRevision, project, file, afterContent);
            } else {
                log.info(project.getName() + " merge-check after content not contains conflict mark");
                String afterKey = project.getName() + file.getPath() + CommonUtil.AFTER;
                String afterContentCache = CacheUtil.PROJECT_CONFLICT_CACHE.getIfPresent(afterKey);
                if (afterContentCache == null) {
                    log.info(project.getName() + " merge-check afterContentCache is null");
                    continue;
                }
                log.info(project.getName() + " merge-check after content:" + afterContent);
                String beforeKey = project.getName() + file.getPath() + CommonUtil.BEFORE;
                String beforeContentCache = CacheUtil.PROJECT_CONFLICT_CACHE.getIfPresent(beforeKey);

                CacheUtil.PROJECT_CONFLICT_CACHE.invalidate(beforeKey);
                CacheUtil.PROJECT_CONFLICT_CACHE.invalidate(afterKey);
                findMissingLines(project, file.getPath(), beforeContentCache, afterContentCache, afterContent);
            }

        }
    }

    public static void findMissingLines(Project project, String file, String beforeContent, String afterContent,
        String nowContent) {
        log.info(file + " merge-check findMissingLines");
        // 分割成行并去除首尾空白
        Set<String> beforeLines =
            Arrays.stream(beforeContent.split(CommonUtil.LINE_BREAK)).map(String::trim).collect(Collectors.toSet());
        List<String> afterLines =
            Arrays.stream(afterContent.split(CommonUtil.LINE_BREAK)).map(String::trim).collect(Collectors.toList());
        Set<String> nowLines =
            Arrays.stream(nowContent.split(CommonUtil.LINE_BREAK)).map(String::trim).collect(Collectors.toSet());

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
                MergeConflictDialog dialog = new MergeConflictDialog(project, remoteText, localText);
                dialog.show();
            });
        }
    }

    private void cacheConflict(ContentRevision beforeRevision, Project project, VirtualFile file, String afterContent) {
        // 获取变更前的代码内容
        String beforeContent = null;
        if (beforeRevision != null) {
            try {
                beforeContent = beforeRevision.getContent();
            } catch (VcsException e) {
                log.error(project.getName() + " merge-check get before content error", e.getMessage());
                return;
            }
        }
        if (beforeContent == null) {
            log.info(project.getName() + " merge-check before content is null");
            return;
        }
        log.info(project.getName() + " merge-check cacheConflict " + beforeContent);
        CacheUtil.PROJECT_CONFLICT_CACHE.put(project.getName() + file.getPath() + CommonUtil.BEFORE, beforeContent);
        CacheUtil.PROJECT_CONFLICT_CACHE.put(project.getName() + file.getPath() + CommonUtil.AFTER, afterContent);
    }
}
