package com.example.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import red.line.vebino.BuildConfig
import red.line.vebino.R

object ExternalIntentHelper {

    private const val SUPPORT_EMAIL_INTERNAL = "gmnarcis@gmail.com"
    const val OFFICIAL_PACKAGE_NAME = "red.line.vebino"
    private const val MYKET_PACKAGE_NAME = "ir.mservices.market"
    const val MYKET_DEVELOPER_URL = "https://myket.ir/developer/dev-36089"
    const val MYKET_APP_WEB_URL = "https://myket.ir/app/red.line.vebino"

    fun buildSupportEmailIntent(subject: String): Intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL_INTERNAL")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL_INTERNAL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

    fun buildMyketReviewIntents(): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("myket://comment?id=$OFFICIAL_PACKAGE_NAME")).apply {
            setPackage(MYKET_PACKAGE_NAME)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("myket://comment?id=$OFFICIAL_PACKAGE_NAME")),
        Intent(Intent.ACTION_VIEW, Uri.parse("myket://details?id=$OFFICIAL_PACKAGE_NAME")).apply {
            setPackage(MYKET_PACKAGE_NAME)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse(MYKET_APP_WEB_URL)),
    )

    fun buildMyketDeveloperIntents(): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse(MYKET_DEVELOPER_URL)).apply {
            setPackage(MYKET_PACKAGE_NAME)
        },
        Intent(Intent.ACTION_VIEW, Uri.parse(MYKET_DEVELOPER_URL)),
    )

    fun openSupportEmail(
        context: Context,
        chooserTitle: String,
        noEmailClientMessage: String,
        onError: (String) -> Unit,
    ) {
        val baseIntent = buildSupportEmailIntent("پشتیبانی وبینو | WEBINO v${BuildConfig.VERSION_NAME}")
        val launched = launchWithResolverOrDirect(context, listOf(baseIntent), wrapChooserTitle = chooserTitle)
        if (!launched) {
            onError(noEmailClientMessage)
        }
    }

    fun openMyketReview(
        context: Context,
        noTargetMessage: String,
        onError: (String) -> Unit,
    ) {
        val launched = launchWithResolverOrDirect(context, buildMyketReviewIntents())
        if (!launched) {
            onError(noTargetMessage)
        }
    }

    fun openMyketDeveloperApps(
        context: Context,
        noTargetMessage: String,
        onError: (String) -> Unit,
    ) {
        val launched = launchWithResolverOrDirect(context, buildMyketDeveloperIntents())
        if (!launched) {
            onError(noTargetMessage)
        }
    }

    private fun launchWithResolverOrDirect(
        context: Context,
        candidates: List<Intent>,
        wrapChooserTitle: String? = null,
    ): Boolean {
        val pm = context.packageManager
        for (candidate in candidates) {
            if (candidate.resolveActivity(pm) != null) {
                val target = if (wrapChooserTitle != null) {
                    Intent.createChooser(candidate, wrapChooserTitle)
                } else {
                    candidate
                }
                target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val started = runCatching { context.startActivity(target) }.isSuccess
                if (started) return true
            }
        }
        return false
    }
}
