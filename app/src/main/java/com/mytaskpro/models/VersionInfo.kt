package com.mytaskpro.models

data class VersionInfo(
    val versionName: String,
    val versionCode: Int? // Make it nullable to accept Int!
)