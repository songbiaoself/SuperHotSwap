package com.coderevolt.util;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class FileOpenerUtil {

    public static void openFileInEditor(Project project, String filePath) {
        // Convert the file path to a VirtualFile object.
        VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(VirtualFileManager.constructUrl("file", filePath));

        if (virtualFile == null || !virtualFile.exists()) {
            // If the file does not exist, show an error notification.
            IdeaNotifyUtil.notify("The file at path '" + filePath + "' does not exist.", NotificationType.ERROR, null, project);
            return;
        }

        // Get the PsiFile corresponding to the VirtualFile.
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            // If the PsiFile cannot be found, show a warning notification.
            IdeaNotifyUtil.notify("Could not find PsiFile for path: " + filePath, NotificationType.ERROR, null, project);
            return;
        }

        // Open the file in the editor.
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.closeFile(virtualFile);
        editorManager.openFile(virtualFile, true); // The second parameter is requestFocus.
    }

}
