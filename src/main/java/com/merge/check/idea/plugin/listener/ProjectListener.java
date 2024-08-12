package com.merge.check.idea.plugin.listener;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class ProjectListener implements ProjectManagerListener {
    private static final Logger log = Logger.getInstance(ProjectListener.class);

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

        ConflictListener.handle(project);
        ChangeListener.handle(project);
    }

}
