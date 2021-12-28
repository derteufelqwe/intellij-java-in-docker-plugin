package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.runconfig.JavaDockerRunConfiguration;
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.runconfig.MyLocatableConfig;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MyForm extends SettingsEditor<MyLocatableConfig> {

    private JPanel myPanel;
    private LabeledComponent<TextFieldWithBrowseButton> myScriptName;
    private JList<String> list1;
    private JRadioButton radioButton1;

    @Override
    protected void resetEditorFrom(MyLocatableConfig demoRunConfiguration) {
        myScriptName.getComponent().setText("demoRunConfiguration.getScriptName()");
    }

    @Override
    protected void applyEditorTo(@NotNull MyLocatableConfig demoRunConfiguration) {
//        demoRunConfiguration.setScriptName(myScriptName.getComponent().getText());
//        demoRunConfiguration.setData(new ArrayList<>(Arrays.asList("hi")));
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myPanel;
    }

    private void createUIComponents() {
        myScriptName = new LabeledComponent<>();
        myScriptName.setComponent(new TextFieldWithBrowseButton());
    }

}