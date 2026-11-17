package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.model.AppTemplateType

@Database(
    entities = [
        ProjectEntity::class,
        ProjectSettingsEntity::class,
        WebSettingsEntity::class,
        BuildTaskEntity::class,
        BuildResultEntity::class,
        SigningKeyMetadataEntity::class,
        BuildLogEntity::class,
        AppTemplateEntity::class,
        RecentWebsiteEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class WebinoDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun buildDao(): BuildDao
    abstract fun signingDao(): SigningDao
    abstract fun templateAndHistoryDao(): TemplateAndHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WebinoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE project_settings ADD COLUMN showStatusBar INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE project_settings ADD COLUMN showNavigationBar INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE project_settings ADD COLUMN backBehavior TEXT NOT NULL DEFAULT 'HISTORY_THEN_EXIT'")
                db.execSQL("ALTER TABLE web_settings ADD COLUMN webSqlEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE web_settings ADD COLUMN clipboardAccessEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE web_settings ADD COLUMN popupBehavior TEXT NOT NULL DEFAULT 'SAME_PAGE'")
            }
        }

        fun getInstance(context: Context): WebinoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebinoDatabase::class.java,
                    "webino_studio.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }

        fun defaultTemplates(): List<AppTemplateEntity> = listOf(
            AppTemplateEntity(
                id = AppTemplateType.STORE.id,
                titleFa = AppTemplateType.STORE.titleFa,
                titleEn = AppTemplateType.STORE.titleEn,
                descriptionFa = AppTemplateType.STORE.subtitleFa,
                primaryHex = AppTemplateType.STORE.primaryHex,
                backgroundHex = AppTemplateType.STORE.backgroundHex,
                recommendedToolbar = "BOTTOM_BAR",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = true,
            ),
            AppTemplateEntity(
                id = AppTemplateType.CORPORATE.id,
                titleFa = AppTemplateType.CORPORATE.titleFa,
                titleEn = AppTemplateType.CORPORATE.titleEn,
                descriptionFa = AppTemplateType.CORPORATE.subtitleFa,
                primaryHex = AppTemplateType.CORPORATE.primaryHex,
                backgroundHex = AppTemplateType.CORPORATE.backgroundHex,
                recommendedToolbar = "TOP_BAR",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = false,
            ),
            AppTemplateEntity(
                id = AppTemplateType.NEWS.id,
                titleFa = AppTemplateType.NEWS.titleFa,
                titleEn = AppTemplateType.NEWS.titleEn,
                descriptionFa = AppTemplateType.NEWS.subtitleFa,
                primaryHex = AppTemplateType.NEWS.primaryHex,
                backgroundHex = AppTemplateType.NEWS.backgroundHex,
                recommendedToolbar = "TOP_BAR",
                recommendedCache = "CACHE_ELSE_NETWORK",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = false,
            ),
            AppTemplateEntity(
                id = AppTemplateType.EDUCATIONAL.id,
                titleFa = AppTemplateType.EDUCATIONAL.titleFa,
                titleEn = AppTemplateType.EDUCATIONAL.titleEn,
                descriptionFa = AppTemplateType.EDUCATIONAL.subtitleFa,
                primaryHex = AppTemplateType.EDUCATIONAL.primaryHex,
                backgroundHex = AppTemplateType.EDUCATIONAL.backgroundHex,
                recommendedToolbar = "BOTTOM_BAR",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = true,
            ),
            AppTemplateEntity(
                id = AppTemplateType.PERSONAL.id,
                titleFa = AppTemplateType.PERSONAL.titleFa,
                titleEn = AppTemplateType.PERSONAL.titleEn,
                descriptionFa = AppTemplateType.PERSONAL.subtitleFa,
                primaryHex = AppTemplateType.PERSONAL.primaryHex,
                backgroundHex = AppTemplateType.PERSONAL.backgroundHex,
                recommendedToolbar = "NONE",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = false,
            ),
            AppTemplateEntity(
                id = AppTemplateType.SOCIAL.id,
                titleFa = AppTemplateType.SOCIAL.titleFa,
                titleEn = AppTemplateType.SOCIAL.titleEn,
                descriptionFa = AppTemplateType.SOCIAL.subtitleFa,
                primaryHex = AppTemplateType.SOCIAL.primaryHex,
                backgroundHex = AppTemplateType.SOCIAL.backgroundHex,
                recommendedToolbar = "FLOATING_PILL",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = false,
            ),
            AppTemplateEntity(
                id = AppTemplateType.BLOG.id,
                titleFa = AppTemplateType.BLOG.titleFa,
                titleEn = AppTemplateType.BLOG.titleEn,
                descriptionFa = AppTemplateType.BLOG.subtitleFa,
                primaryHex = AppTemplateType.BLOG.primaryHex,
                backgroundHex = AppTemplateType.BLOG.backgroundHex,
                recommendedToolbar = "BOTTOM_BAR",
                recommendedCache = "CACHE_ELSE_NETWORK",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = false,
            ),
            AppTemplateEntity(
                id = AppTemplateType.SERVICE.id,
                titleFa = AppTemplateType.SERVICE.titleFa,
                titleEn = AppTemplateType.SERVICE.titleEn,
                descriptionFa = AppTemplateType.SERVICE.subtitleFa,
                primaryHex = AppTemplateType.SERVICE.primaryHex,
                backgroundHex = AppTemplateType.SERVICE.backgroundHex,
                recommendedToolbar = "TOP_BAR",
                recommendedCache = "DEFAULT",
                recommendedJs = true,
                recommendedDom = true,
                recommendedPaymentBridge = true,
            ),
        )
    }
}
