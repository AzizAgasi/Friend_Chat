package com.techdot.friendchat.notification

data class PushNotification(
    val data: NotificationData,
    val to: String?
)