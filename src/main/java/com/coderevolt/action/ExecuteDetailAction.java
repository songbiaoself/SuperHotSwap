package com.coderevolt.action;

import com.coderevolt.Constant;
import com.coderevolt.util.FileOpenerUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExecuteDetailAction extends AnAction {

    private String fileName;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyMMdd");

    public ExecuteDetailAction(String fileName) {
        super("查看详情");
        this.fileName = fileName;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        FileOpenerUtil.openFileInEditor(e.getProject(), Paths.get(Constant.homePath, "log", dateFormat.format(new Date()), fileName).toString());
    }
}
