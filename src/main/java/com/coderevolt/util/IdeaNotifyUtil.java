package com.coderevolt.util;

import com.intellij.notification.*;

import java.util.UUID;

public class IdeaNotifyUtil {

    public static void notify(String content, NotificationType type) {
        NotificationGroup notificationGroup = new NotificationGroup(UUID.randomUUID().toString().replace("-", ""), NotificationDisplayType.BALLOON, true);
        Notification notification = notificationGroup.createNotification(content, type);
        Notifications.Bus.notify(notification);
    }

}
