package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppLanguage
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.Webino3DGlassIcon
import com.example.ui.components.Webino3DIconType
import com.example.ui.components.WebinoAmbientBackground
import com.example.ui.components.localizeNumber
import com.example.ui.theme.AuroraTeal
import com.example.ui.theme.CoralRed
import com.example.ui.theme.GrapePurple
import com.example.ui.theme.GrapeSoft
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OrchidDeep
import com.example.ui.theme.RosePink
import com.example.ui.theme.SunriseAmber
import com.example.ui.theme.SunsetOrange
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: Webino3DIconType,
    val titleFa: String,
    val titleEn: String,
    val bodyFa: String,
    val bodyEn: String,
    val from: Color,
    val to: Color,
)

private val pages = listOf(
    OnboardingPage(
        Webino3DIconType.HOME,
        "سایتت را به اپلیکیشن تبدیل کن",
        "Turn your website into an app",
        "وبینو مستقیماً روی گوشی خودت APK و AAB می‌سازد؛ بدون کامپیوتر و بدون سرور.",
        "WEBINO builds APK and AAB right on your phone: no computer, no server.",
        SunsetOrange, RosePink,
    ),
    OnboardingPage(
        Webino3DIconType.PROJECTS,
        "۱. یک پروژه بساز",
        "1. Create a project",
        "آدرس سایتت را وارد کن. نام و اطلاعات سایت خودکار خوانده می‌شود و می‌توانی از قالب‌های آماده هم شروع کنی.",
        "Enter your website address. Its name and details are read automatically, or start from a ready template.",
        GrapeSoft, GrapePurple,
    ),
    OnboardingPage(
        Webino3DIconType.CUSTOMIZE,
        "۲. ظاهر اپ را شخصی‌سازی کن",
        "2. Customize the look",
        "آیکون، رنگ، صفحه‌ی شروع و تنظیمات وب را تغییر بده و با «پیش‌نمایش» نتیجه را همان لحظه ببین.",
        "Change icon, colors, splash and web settings, and check the result instantly in the preview.",
        AuroraTeal, OceanBlue,
    ),
    OnboardingPage(
        Webino3DIconType.BUILD,
        "۳. بساز؛ حتی در پس‌زمینه",
        "3. Build, even in background",
        "ساخت را شروع کن و برنامه را ببند. پیشرفت در اعلان نمایش داده می‌شود و در پایان خبرت می‌کند.",
        "Start a build and leave the app. Progress shows in a notification and you are told when it ends.",
        OrchidDeep, RosePink,
    ),
    OnboardingPage(
        Webino3DIconType.SIGNING,
        "۴. امضا کن و نگه‌دار",
        "4. Sign and keep your key",
        "برای انتشار آپدیت باید همیشه با همان کلید امضا کنی. کلید امضا را در جای امن نگه دار. اگر ساختی خطا داد، از «تاریخچه‌ی ساخت» لاگ را برای پشتیبانی بفرست.",
        "Updates must always be signed with the same key, so keep it safe. If a build fails, send its log to support from Build history.",
        SunriseAmber, CoralRed,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    language: AppLanguage,
    usePersianDigits: Boolean,
    onFinished: () -> Unit,
) {
    val isFa = language == AppLanguage.FA
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    WebinoAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .testTag("onboarding_root"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localizeNumber(pagerState.currentPage + 1, language, usePersianDigits) +
                        " / " + localizeNumber(pages.size, language, usePersianDigits),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
                TextButton(onClick = onFinished, modifier = Modifier.testTag("onboarding_skip")) {
                    Text(if (isFa) "رد کردن" else "Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(188.dp)
                            .shadow(24.dp, CircleShape, ambientColor = page.to.copy(alpha = 0.5f), spotColor = page.to.copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(page.from, page.to)))
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.1f))),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(124.dp)
                                .clip(RoundedCornerShape(38.dp))
                                .background(Color.White.copy(alpha = 0.30f))
                                .border(1.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(38.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Webino3DGlassIcon(type = page.icon, size = 96.dp)
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(
                        text = if (isFa) page.titleFa else page.titleEn,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (isFa) page.bodyFa else page.bodyEn,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEachIndexed { i, _ ->
                    val selected = i == pagerState.currentPage
                    val w by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dotW")
                    val a by animateFloatAsState(if (selected) 1f else 0.35f, label = "dotA")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .size(width = w, height = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(GrapePurple.copy(alpha = a)),
                    )
                }
            }

            GradientPrimaryButton(
                text = when {
                    isLast -> if (isFa) "شروع کنیم" else "Get started"
                    else -> if (isFa) "بعدی" else "Next"
                },
                icon = if (isLast) Icons.Rounded.RocketLaunch else if (isFa) Icons.Rounded.ArrowBack else Icons.Rounded.ArrowForward,
                onClick = {
                    if (isLast) onFinished()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                testTag = "onboarding_next",
            )
        }
    }
}
