package com.agentasker

import android.app.Application
import com.agentasker.core.di.AppContainer

class AgentTaskerApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

