package com.agentasker.core.navigation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import com.agentasker.features.login.navigation.NavigationWrapperAuth
import com.agentasker.features.dashboard.navigation.NavigationWrapperDashboard
import com.agentasker.features.tasks.navigation.NavigationWrapperTasks
import com.agentasker.features.classroom.navigation.NavigationWrapperClassroom
import com.agentasker.features.kanban.navigation.NavigationWrapperKanban
import com.agentasker.features.analyzer.navigation.NavigationWrapperAnalyzer

@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationModule {

    @Binds
    @IntoSet
    abstract fun bindAuthNavGraph(impl: NavigationWrapperAuth): FeatureNavGraph

    @Binds
    @IntoSet
    abstract fun bindDashboardNavGraph(impl: NavigationWrapperDashboard): FeatureNavGraph

    @Binds
    @IntoSet
    abstract fun bindTasksNavGraph(impl: NavigationWrapperTasks): FeatureNavGraph

    @Binds
    @IntoSet
    abstract fun bindClassroomNavGraph(impl: NavigationWrapperClassroom): FeatureNavGraph

    @Binds
    @IntoSet
    abstract fun bindKanbanNavGraph(impl: NavigationWrapperKanban): FeatureNavGraph

    @Binds
    @IntoSet
    abstract fun bindAnalyzerNavGraph(impl: NavigationWrapperAnalyzer): FeatureNavGraph
}
