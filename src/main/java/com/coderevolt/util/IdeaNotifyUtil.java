package com.coderevolt.util;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public class IdeaNotifyUtil {

    private static Notification getNotification(String content, NotificationType type) {
        if (content != null && type != null) {
            NotificationGroupManager groupManager = NotificationGroupManager.getInstance();
            NotificationGroup notificationGroup = groupManager.getNotificationGroup("notifyAction");
            return notificationGroup.createNotification("SuperHotSwap", content, type);
        }
        throw new IllegalArgumentException("Notification type or content is null");
    }

    public static Notification notify(String content, NotificationType type, @Nullable AnAction action, @Nullable Project project) {
        Notification notification = getNotification(content, type);
        if (action != null) {
            notification.addAction(action);
        }
        Notifications.Bus.notify(notification, project);
        return notification;
    }

}
