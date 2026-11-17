# مستندات فنی و راهنمای ساخت WEBINO (وبینو)

## ۱. نتیجه امکانسنجی فنی On-Device Build روی اندروید

### چالشهای اجرای Toolchain دسکتاپ روی گوشی اندرویدی
1. **Gradle و Android Gradle Plugin (AGP):** نیازمند JDK کامل دسکتاپ، Gradle Daemon، فضای دیسک بالا (بیش از ۱.۵ گیگابایت) و رم ۲ تا ۴ گیگابایت هستند که در محیط Sandbox و محدودیت حافظه اپلیکیشنهای اندروید قابل اجرا نیستند.
2. **باینریهای بومی AAPT2 و D8/R8 دسکتاپ:** در اندروید ۱۰ به بالا (`targetSdk >= 29`) به دلیل محدودیت امنیتی SELinux (`W^X` و `noexec` روی دایرکتوریهای داده برنامه)، اجرای فایلهای باینری خارجی دانلودشده در زمان اجرا ممنوع و غیرممکن است.
3. **عدم دسترسی به `.class` در `/system/framework`:** در اندروید ۱۰+، کتابخانههای سیستم تنها شامل `.dex` هستند و کامپایلر جاوا برای کامپایل به فایلهای `.class` کتابخانه پایه نیاز دارد.

### راهکار واقعی و اثباتشده وبینو (۱۰۰٪ محلی و بدون سرور)
وبینو به جای کپی کردن APK آماده یا وابستگی به سرور ابری، یک **زنجیره کامپایل و بستهبندی خالص JVM درونفرایند (In-Process)** را پیادهسازی کرده است:

1. **کامپایل سورس جاوا (`JavaCompiler.kt`):** استفاده از `Eclipse JDT Core Batch Compiler (ECJ)` در کنار کتابخانه فشرده `assets/compiler/android-stubs.jar` (۶۴ کیلوبایت) برای تولید فایلهای واقعی `.class`.
2. **تولید بایتکد اندروید (`DexGenerator.kt`):** تبدیل `.class` به `classes.dex` استاندارد با موتور `dalvik-dx`.
3. **کامپایل باینری مانیفست و منابع (`AxmlCompiler.kt` و `ArscCompiler.kt`):** تولید واقعی `AndroidManifest.xml` باینری (`RES_XML_TYPE`) و `resources.arsc` (`RES_TABLE_TYPE`) همراه با نام برنامه، آیکونهای سفارشی و رنگها.
4. **بستهبندی و همترازی ۴بایتی (`ApkPackager.kt`):** ذخیره غیرفشرده (`STORED`) برای `resources.arsc` و تصاویر PNG همراه با تراز ۴بایتی ZipAlign (الزامی برای اندروید ۱۱+).
5. **امضای دیجیتال و اعتبارسنجی (`ApkSignerEngine.kt` و `OutputValidator.kt`):** امضای رمزنگاری v1 + v2 + v3 با کتابخانه رسمی `Google apksig` و راستیآزمایی بلافاصله با `ApkVerifier`.
6. **ساخت و امضای بسته انتشار (`AabPackager.kt` و `ProtoAabEncoder.kt`):** تولید فایل `.aab` شامل `BundleConfig.pb`، مانیفست و جدول منابع Protobuf و امضای CMS/PKCS#7 با `BouncyCastle`.

---

## ۲. معماری پروژه (Clean Architecture + MVVM)

- `com.example.core`: تقویم هجری شمسی (`JalaliCalendar`)، تبدیل ارقام فارسی، اعتبارسنجی URL/HTTPS/دامنه `.ir` و نام بسته.
- `com.example.domain`: مدلهای دامنه، ۸ قالب تجاری آماده و مراحل ۸گانه موتور ساخت.
- `com.example.data`:
  - `local.db`: پایگاه داده `Room` با ۹ جدول (`ProjectEntity`, `ProjectSettingsEntity`, `WebSettingsEntity`, `BuildTaskEntity`, `BuildResultEntity`, `SigningKeyMetadataEntity`, `BuildLogEntity`, `AppTemplateEntity`, `RecentWebsiteEntity`).
  - `local.prefs`: تنظیمات کاربر با `DataStore Preferences`.
  - `crypto`: مدیریت `PKCS#12 Keystore` و حفاظت رمزها با `AndroidKeyStore AES-GCM`.
  - `network`: استخراج عنوان، توضیحات، Favicon و رنگ قالب وبسایت با `OkHttp + Jsoup`.
- `com.example.buildengine`: موتور ساخت مستقل از رابط کاربری.
- `com.example.webviewengine`: موتور WebView مستقل با سیاستهای سختگیرانه SSL، کنترل Origin و مدیریت فایل/دانلود.
- `com.example.work`: `WorkManager CoroutineWorker` برای اجرای پایدار Build در پسزمینه.
- `com.example.ui`: رابط کاربری مدرن Jetpack Compose Material 3 با پشتیبانی کامل RTL و اندازههای مختلف نمایشگر.

---

## ۳. راهنمای دریافت APK / AAB خود اپلیکیشن وبینو

برای دریافت نسخه نهایی Release خود اپلیکیشن وبینو:
- در منوی تنظیمات AI Studio از گزینه Export / Generate APK یا AAB استفاده کنید، یا در محیط محلی دستور زیر را اجرا کنید:
  ```bash
  gradle :app:assembleRelease
  gradle :app:bundleRelease
  ```
- برای اجرای تستهای خودکار:
  ```bash
  gradle :app:testDebugUnitTest
  ```
