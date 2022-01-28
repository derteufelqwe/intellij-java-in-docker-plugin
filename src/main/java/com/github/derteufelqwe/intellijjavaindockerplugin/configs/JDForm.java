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

    private JPanel javaConfig;
    private LabeledComponent<ClassEditorField> mainClass;

    private JPanel dockerConfig;
    private JCheckBox reuseContainer;
    private LabeledComponent<JTextField> dockerImage;
    private LabeledComponent<JTextField> internalContainer;
    private JCheckBox removeContainer;

    private JPanel existingContainerConfig;
    private LabeledComponent<JTextField> existingContainerId;
    private JCheckBox useExistingContainer;


    public JDForm(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        mainClass.getComponent().setText(options.getMainClass());
        dockerImage.getComponent().setText(options.getDockerImage());
        reuseContainer.setSelected(options.getReuseContainer());
        removeContainer.setSelected(options.getRemoveContainerOnStop());
        existingContainerId.getComponent().setText(options.getContainerId());
        internalContainer.getComponent().setText(options.getHiddenContainerId());
        useExistingContainer.setSelected(options.getUseExistingContainer());

        internalContainer.getComponent().setEnabled(false);
        // Disable the default docker config panel when using an existing container
        if (useExistingContainer.isSelected()) {
            setDockerConfigEnabled(false);
        }
        useExistingContainer.addActionListener(e -> {
            setDockerConfigEnabled(!((JCheckBox) e.getSource()).isSelected());
        });
    }

    @Override
    protected void applyEditorTo(@NotNull JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        options.setMainClass(mainClass.getComponent().getText());
        options.setDockerImage(dockerImage.getComponent().getText());
        options.setReuseContainer(reuseContainer.isSelected());
        options.setRemoveContainerOnStop(removeContainer.isSelected());
        options.setContainerId(existingContainerId.getComponent().getText());
        options.setUseExistingContainer(useExistingContainer.isSelected());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    private void createUIComponents() {
        mainClass = new LabeledComponent<>();
        mainClass.setComponent(ClassEditorField.createClassField(project, () -> null, JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE, null));

        internalContainer = new LabeledComponent<>();
        internalContainer.setComponent(new JTextField());
        internalContainer.getComponent().setEnabled(false);

        dockerImage = new LabeledComponent<>();
        dockerImage.setComponent(new JTextField());

        existingContainerId = new LabeledComponent<>();
        existingContainerId.setComponent(new JTextField());
    }


    private void setDockerConfigEnabled(boolean enabled) {
        dockerImage.setEnabled(enabled);
        reuseContainer.setEnabled(enabled);
        removeContainer.setEnabled(enabled);
    }

}