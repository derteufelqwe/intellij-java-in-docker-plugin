package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.intellij.execution.application.ClassEditorField;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaCodeFragment;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JDForm extends SettingsEditor<JDRunConfiguration> {

    private final Project project;

    private JPanel myPanel;
    private LabeledComponent<ClassEditorField> mainClass;
    private JCheckBox reuseContainer;
    private JPanel javaConfig;
    private JPanel dockerConfig;
    private LabeledComponent<JTextField> dockerImage;
    private LabeledComponent<JTextField> containerId;
    private JCheckBox removeContainer;
    private LabeledComponent<JTextField> internalContainer;


    public JDForm(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        mainClass.getComponent().setText(options.getMainClass());
        dockerImage.getComponent().setText(options.getDockerImage());
        reuseContainer.setSelected(options.getReuseContainer());
        removeContainer.setSelected(options.getRemoveContainer());
        containerId.getComponent().setText(options.getContainerId());
        internalContainer.getComponent().setText(options.getHiddenContainerId());

        internalContainer.getComponent().setEnabled(false);
    }

    @Override
    protected void applyEditorTo(@NotNull JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        options.setMainClass(mainClass.getComponent().getText());
        options.setDockerImage(dockerImage.getComponent().getText());
        options.setReuseContainer(reuseContainer.isSelected());
        options.setRemoveContainer(removeContainer.isSelected());
        options.setContainerId(containerId.getComponent().getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    private void createUIComponents() {
        mainClass = new LabeledComponent<>();
//        mainClass.setComponent(new EditorTextFieldWithBrowseButton(project, true));
        mainClass.setComponent(ClassEditorField.createClassField(project, () -> null, JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE, null));

        internalContainer = new LabeledComponent<>();
        internalContainer.setComponent(new JTextField());
        internalContainer.getComponent().setEnabled(false);

        dockerImage = new LabeledComponent<>();
        dockerImage.setComponent(new JTextField());

        containerId = new LabeledComponent<>();
        containerId.setComponent(new JTextField());
    }

}