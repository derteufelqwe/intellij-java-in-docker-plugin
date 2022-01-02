package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JDForm extends SettingsEditor<JDRunConfiguration> {

    private JPanel myPanel;
    private LabeledComponent<TextFieldWithBrowseButton> mainClass;
    private JCheckBox reuseContainer;
    private JPanel javaConfig;
    private JPanel dockerConfig;
    private LabeledComponent<JTextField> dockerImage;
    private LabeledComponent<JTextField> containerId;
    private JCheckBox removeContainer;


    @Override
    protected void resetEditorFrom(JDRunConfiguration config) {
        JDRunConfigurationOptions options = config.getOptions();

        mainClass.getComponent().setText(options.getMainClass());
        dockerImage.getComponent().setText(options.getDockerImage());
        reuseContainer.setSelected(options.getReuseContainer());
        removeContainer.setSelected(options.getRemoveContainer());
        containerId.getComponent().setText(options.getContainerId());
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
        mainClass.setComponent(new TextFieldWithBrowseButton());

        dockerImage = new LabeledComponent<>();
        dockerImage.setComponent(new JTextField());

        containerId = new LabeledComponent<>();
        containerId.setComponent(new JTextField());
    }

}