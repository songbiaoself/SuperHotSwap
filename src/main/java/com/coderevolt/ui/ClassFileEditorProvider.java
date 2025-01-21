package com.coderevolt.ui;

import com.coderevolt.context.ProjectContext;
import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ClassFileEditorProvider extends TextEditorProvider {

    private static final String id = "SuperHotSwap";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        String path = file.getPath();
        return "class".equalsIgnoreCase(file.getExtension()) || path.contains(".zip!/") || path.contains(".jar!/");
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        try {
            String content = ProjectContext.load(project, file.getPath(), () -> {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                return psiFile == null ? "occur error!!!" : psiFile.getText();
            });
            Language language = Language.findLanguageByID("JAVA");
            LightVirtualFile lightVirtualFile = new LightVirtualFile(file.getName(), new LanguageFileType(language == null ? new Language("JAVA") {} : language) {
                @Override
                public @NonNls @NotNull String getName() {
                    return file.getName();
                }

                @Override
                public @NlsContexts.Label @NotNull String getDescription() {
                    return "Byte class hotswap";
                }

                @Override
                public @NlsSafe @NotNull String getDefaultExtension() {
                    return "class";
                }

                @Override
                public @Nullable Icon getIcon() {
                    return null;
                }

            }, content) {

                @Override
                public @Nullable @NlsSafe String getExtension() {
                    return file.getExtension();
                }

                @Override
                public @NotNull String getPath() {
                    return file.getPath();
                }

            };
            return new ClassTextEditor(new PsiAwareTextEditorImpl(project, lightVirtualFile, this));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull @NonNls String getEditorTypeId() {
        return id;
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
}
