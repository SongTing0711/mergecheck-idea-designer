package com.merge.check.idea.plugin.listener;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;

/**
 * @Author : song hao
 * @CreateTime : 2024/07/04 19:30
 * @Description :
 */
public class MergeCheckProjectComponent implements ProjectComponent {
    private final Project project;

    public MergeCheckProjectComponent(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        VirtualFileManager.getInstance().addVirtualFileListener(new ConflictListener(), project);
    }

}