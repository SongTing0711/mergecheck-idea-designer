package com.merge.check.idea.plugin.dialog;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * @Author : song hao
 * @CreateTime : 2024/08/05 19:55
 * @Description :
 */
public class MergeConflictDialog extends DialogWrapper {
    private final String remoteMissingLinesText;
    private final String localMissingLinesText;
    private final Project project;
    private Editor remoteEditor;
    private Editor localEditor;

    public MergeConflictDialog(String file, Project project, String remoteMissingLinesText,
        String localMissingLinesText) {
        super(project);
        setTitle(file + "合并丢失");
        this.project = project;
        this.remoteMissingLinesText = remoteMissingLinesText;
        this.localMissingLinesText = localMissingLinesText;
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create editor components for better code presentation
        remoteEditor = createEditor(remoteMissingLinesText);
        localEditor = createEditor(localMissingLinesText);

        // Add labels and editors to the panel
        panel.add(new JBLabel("远程丢失"));
        panel.add(createEditorComponent(remoteEditor));
        panel.add(Box.createVerticalStrut(10)); // Spacer
        panel.add(new JBLabel("本地丢失"));
        panel.add(createEditorComponent(localEditor));

        return panel;
    }

    private Editor createEditor(String text) {
        // Create a document from the text
        Document document = EditorFactory.getInstance().createDocument(text);

        // Create an editor from the document
        Editor editor = EditorFactory.getInstance().createEditor(document, project);

        // Customize the editor settings
        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setLineNumbersShown(true);
        editorSettings.setFoldingOutlineShown(false);
        editorSettings.setIndentGuidesShown(false);

        return editor;
    }

    private JComponent createEditorComponent(Editor editor) {
        // Wrap the editor in a JBScrollPane
        JBScrollPane scrollPane = new JBScrollPane(editor.getComponent());
        scrollPane.setBorder(BorderFactory.createLineBorder(JBColor.border()));
        return scrollPane;
    }

    @Override
    public void dispose() {
        // Dispose the editors to avoid memory leaks
        if (remoteEditor != null) {
            EditorFactory.getInstance().releaseEditor(remoteEditor);
            remoteEditor = null;
        }
        if (localEditor != null) {
            EditorFactory.getInstance().releaseEditor(localEditor);
            localEditor = null;
        }
        super.dispose();
    }
}