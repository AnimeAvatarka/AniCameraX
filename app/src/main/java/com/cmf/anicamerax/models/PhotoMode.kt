package com.cmf.anicamerax.models

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cmf.anicamerax.R

enum class PhotoMode(@StringRes val titleRes: Int) {
    Camera(R.string.mode_camera),
    Night(R.string.mode_night),
    Portrait(R.string.mode_portrait),
    HDR(R.string.mode_hdr),
    Video(R.string.mode_video),
    Front(R.string.mode_front);

    @get:Composable
    val title: String
        get() = stringResource(titleRes)

    companion object {
        val entries = listOf(Camera, Night, Portrait, HDR, Video, Front)
    }
}