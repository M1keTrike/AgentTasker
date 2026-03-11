package com.agentasker.features.classroom.data.datasources.remote.model

import com.google.gson.annotations.SerializedName

data class ClassroomConnectRequestDTO(
    @SerializedName("idToken") val idToken: String,
    @SerializedName("authorizationCode") val authorizationCode: String
)

data class ClassroomCourseDTO(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("section") val section: String?,
    @SerializedName("descriptionHeading") val descriptionHeading: String?,
    @SerializedName("courseState") val courseState: String,
    @SerializedName("alternateLink") val alternateLink: String
)

data class ClassroomTaskDTO(
    @SerializedName("id") val id: String,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("courseName") val courseName: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("dueDate") val dueDate: String?,
    @SerializedName("submissionState") val submissionState: String,
    @SerializedName("alternateLink") val alternateLink: String?,
    @SerializedName("maxPoints") val maxPoints: Double?
)

data class ClassroomStatusDTO(
    @SerializedName("connected") val connected: Boolean
)
