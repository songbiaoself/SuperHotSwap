package com.coderevolt.util;

import com.intellij.notification.*;

public class IdeaNotifyUtil {

    public static void notify(String content, NotificationType type) {
        if (content != null && type != null) {
            NotificationGroupManager groupManager = NotificationGroupManager.getInstance();
            NotificationGroup notificationGroup = groupManager.getNotificationGroup("notifyAction");
            Notification notification = notificationGroup.createNotification(content, type);
            Notifications.Bus.notify(notification);
        }
    }

}
