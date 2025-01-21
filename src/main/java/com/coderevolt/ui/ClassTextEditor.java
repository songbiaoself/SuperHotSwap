package com.coderevolt.ui;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class ClassTextEditor implements TextEditor {

    private final TextEditor textEditor;

    public ClassTextEditor(TextEditor textEditor) {
        this.textEditor = textEditor;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return textEditor.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return textEditor.getPreferredFocusedComponent();
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "SuperHotSwap";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {
        textEditor.setState(state);
    }

    @Override
    public boolean isModified() {
        return textEditor.isModified();
    }

    @Override
    public boolean isValid() {
        return textEditor.isValid();
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
        textEditor.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
        textEditor.removePropertyChangeListener(listener);
    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return textEditor.getCurrentLocation();
    }

    @Override
    public void dispose() {
        textEditor.dispose();
    }

    @Override
    public <T> @Nullable T getUserData(@NotNull Key<T> key) {
        return textEditor.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        textEditor.putUserData(key, value);
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return textEditor.getFile();
    }

    @Override
    public @NotNull Editor getEditor() {
        return textEditor.getEditor();
    }

    @Override
    public boolean canNavigateTo(@NotNull Navigatable navigatable) {
        return textEditor.canNavigateTo(navigatable);
    }

    @Override
    public void navigateTo(@NotNull Navigatable navigatable) {
        textEditor.navigateTo(navigatable);
    }

    @Override
    public @NotNull FileEditorState getState(@NotNull FileEditorStateLevel level) {
        return textEditor.getState(level);
    }

    @Override
    public void setState(@NotNull FileEditorState state, boolean exactState) {
        textEditor.setState(state, exactState);
    }

    @Override
    public void selectNotify() {
        textEditor.selectNotify();
    }

    @Override
    public void deselectNotify() {
        textEditor.deselectNotify();
    }

    @Override
    public @Nullable StructureViewBuilder getStructureViewBuilder() {
        return textEditor.getStructureViewBuilder();
    }
}
