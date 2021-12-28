package com.github.derteufelqwe.intellijjavaindockerplugin

import com.intellij.DynamicBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.OrderEnumerator
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.MyBundle"

object MyBundle : DynamicBundle(BUNDLE) {

    @JvmStatic
    val CONTAINER_ID = "3e45e720f774e2ca8ea4b221e29e279b04a267991c234d1b00a34fb8b6e44edf"

    @JvmStatic
    val PROCESS_ID = "jd_process_id"


    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}


//        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
//        JBPopupFactory.getInstance()
//                .createHtmlTextBalloonBuilder("Dis is bad text", MessageType.INFO, null)
//                .setFadeoutTime(7500)
//                .createBalloon()
//                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.above);

//val module = ModuleManager.getInstance(project).modules[0]
//val en = OrderEnumerator.orderEntries(project).recursively()

//val n = Notification("Java Docker", "Content", NotificationType.ERROR)
//Notifications.Bus.notify(n)