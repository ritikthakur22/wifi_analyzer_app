package com.rtwifianalyzer.app.di

import android.content.Context
import android.net.wifi.WifiManager
import androidx.room.Room
import com.rtwifianalyzer.app.data.DeviceDatabase
import com.rtwifianalyzer.app.data.SettingsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun wifi(@ApplicationContext context: Context) = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    @Provides @Singleton fun settings(@ApplicationContext context: Context) = SettingsStore(context)
    @Provides @Singleton fun database(@ApplicationContext context: Context): DeviceDatabase = Room.databaseBuilder(context, DeviceDatabase::class.java, "wifi-analyzer.db").fallbackToDestructiveMigration().build()
}
