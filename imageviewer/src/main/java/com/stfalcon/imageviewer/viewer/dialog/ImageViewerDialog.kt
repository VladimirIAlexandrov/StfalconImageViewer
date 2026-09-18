/*
 * Copyright 2018 stfalcon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://apache.org
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stfalcon.imageviewer.viewer.dialog

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import com.stfalcon.imageviewer.viewer.builder.BuilderData
import com.stfalcon.imageviewer.viewer.view.ImageViewerView

internal class ImageViewerDialog<T>(
    context: Context,
    private val builderData: BuilderData<T>
) {

    private val dialog: AlertDialog
    private val viewerView: ImageViewerView<T> = ImageViewerView(context)
    private var animateOpen = true

    // Принудительно отдаем диалогу нативный полноэкранный стиль, 
    // который ломает жесткие рамки контейнера модальных окон на уровне ОС
    private val dialogStyle: Int
        get() = android.R.style.Theme_Translucent_NoTitleBar_Fullscreen

    init {
        setupViewerView()
        dialog = AlertDialog
            .Builder(context, dialogStyle)
            .setView(viewerView)
            .setOnKeyListener { _, keyCode, event -> onDialogKeyEvent(keyCode, event) }
            .create()
            .apply {
                setOnShowListener { viewerView.open(builderData.transitionView, animateOpen) }
                setOnDismissListener { builderData.onDismissListener?.onDismiss() }
            }
    }

    fun show(animate: Boolean) {
        animateOpen = animate

        // Сначала вызываем show, чтобы окно физически материализовалось в системе
        dialog.show()

        val window = dialog.window ?: return

        // Убираем скрытые отступы темы AlertDialog (делаем фон подложки прозрачным)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // Включаем честный нативный Edge-to-Edge для этого окна
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Разрешаем окну использовать область вокруг выреза камеры (челки)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val layoutParams = window.attributes
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = layoutParams
        }

        // Жестко растягиваем окно на весь экран, перебивая любые дефолтные лимиты
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    fun close() {
        viewerView.close()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun updateImages(images: List<T>) {
        viewerView.updateImages(images)
    }

    fun getCurrentPosition(): Int =
        viewerView.currentPosition

    fun setCurrentPosition(position: Int): Int {
        viewerView.currentPosition = position
        return viewerView.currentPosition
    }

    fun updateTransitionImage(imageView: ImageView?) {
        viewerView.updateTransitionImage(imageView)
    }

    private fun onDialogKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK &&
            event.action == KeyEvent.ACTION_UP &&
            !event.isCanceled
        ) {
            if (viewerView.isScaled) {
                viewerView.resetScale()
            } else {
                viewerView.close()
            }
            return true
        }
        return false
    }

    private fun setupViewerView() {
        viewerView.apply {
            isZoomingAllowed = builderData.isZoomingAllowed
            isSwipeToDismissAllowed = builderData.isSwipeToDismissAllowed

            containerPadding = builderData.containerPaddingPixels
            imagesMargin = builderData.imageMarginPixels
            overlayView = builderData.overlayView

            setBackgroundColor(builderData.backgroundColor)
            setImages(builderData.images, builderData.startPosition, builderData.imageLoader)

            onPageChange = { position -> builderData.imageChangeListener?.onImageChange(position) }
            onDismiss = { dialog.dismiss() }
        }
    }
}
