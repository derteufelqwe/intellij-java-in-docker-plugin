package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.intellij.execution.application.ClassEditorField;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExpandableTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JDForm extends SettingsEditor<JDRunConfiguration> {

    private final Project project;

    private JPanel myPanel;

    private JPanel javaConfig;
    private LabeledComponent<ClassEditorField> mainClass;
    private LabeledComponent<JTextField> jvmArgs;
    private LabeledComponent<JTextField> javaParams;

    private JPanel dockerConfig;
    private JCheckBox reuseContainer;
    private LabeledComponent<JTextField> dockerImage;
    private LabeledComponent<JTextField> internalContainer;
    private JCheckBox removeContainer;
    private LabeledComponent<JTextField> exposedPorts;
    private LabeledComponent<ExpandableTextField> mounts;

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
        jvmArgs.getComponent().setText(options.getJvmArgs());
        javaParams.getComponent().setText(options.getJavaParams());
        dockerImage.getComponent().setText(options.getDockerImage());
        reuseContainer.setSelected(options.getReuseContainer());
        removeContainer.setSelected(options.getRemoveContainerOnStop());
        existingContainerId.getComponent().setText(options.getContainerId());
        internalContainer.getComponent().setText(options.getHiddenContainerId());
        useExistingContainer.setSelected(options.getUseExistingContainer());
        exposedPorts.getComponent().setText(options.getExposedPorts());
        mounts.getComponent().setText(options.getMounts());


        internalContainer.getComponent().setEnabled(false);
        // Disable the default docker config panel when using an existing container
        if (useExistingContainer.isSelected()) {
            setDockerConfigEnabled(false);
        } else {
            existingContainerId.setEnabled(false);
        }

        useExistingContainer.addActionListener(e -> {
            JCheckBox cb = (JCheckBox) e.getSource();
            setDockerConfigEnabled(!cb.isSelected());
            existingContainerId.setEnabled(cb.isSelected());
        });


    }

    @Override
    protected void applyEditorTo(@NotNull JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        options.setMainClass(mainClass.getComponent().getText());
        options.setJvmArgs(jvmArgs.getComponent().getText());
        options.setJavaParams(javaParams.getComponent().getText());
        options.setDockerImage(dockerImage.getComponent().getText());
        options.setReuseContainer(reuseContainer.isSelected());
        options.setRemoveContainerOnStop(removeContainer.isSelected());
        options.setContainerId(existingContainerId.getComponent().getText());
        options.setUseExistingContainer(useExistingContainer.isSelected());
        options.setExposedPorts(exposedPorts.getComponent().getText());
        options.setMounts(mounts.getComponent().getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    private void createUIComponents() {
        // Java part
        mainClass = new LabeledComponent<>();
        mainClass.setComponent(ClassEditorField.createClassField(project, () -> null, JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE, null));

        jvmArgs = new LabeledComponent<>();
        jvmArgs.setComponent(new JTextField());

        javaParams = new LabeledComponent<>();
        javaParams.setComponent(new JTextField());

        // Docker part
        internalContainer = new LabeledComponent<>();
        internalContainer.setComponent(new JTextField());
        internalContainer.getComponent().setEnabled(false);

        dockerImage = new LabeledComponent<>();
        dockerImage.setComponent(new JTextField());

        existingContainerId = new LabeledComponent<>();
        existingContainerId.setComponent(new JTextField());

        exposedPorts = new LabeledComponent<>();
        exposedPorts.setComponent(new JTextField());

        mounts = new LabeledComponent<>();
        mounts.setComponent(new ExpandableTextField());

    }

    private void setDockerConfigEnabled(boolean enabled) {
        dockerImage.setEnabled(enabled);
        reuseContainer.setEnabled(enabled);
        removeContainer.setEnabled(enabled);
    }

}