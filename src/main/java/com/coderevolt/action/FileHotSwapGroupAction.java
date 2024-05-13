package com.coderevolt.action;

import com.coderevolt.context.MachineBeanInfo;
import com.coderevolt.context.VirtualMachineContext;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 公众号: CodeRevolt
 * @date 2024/5/13 14:15
 * @description 分组action
 */
public class FileHotSwapGroupAction extends ActionGroup {
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        for (MachineBeanInfo process : VirtualMachineContext.values()) {
            actions.add(new ProjectAction(process.getProcessName()));
        }
        return actions.toArray(new AnAction[0]);
}

}
