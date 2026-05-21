package com.liuxiugame.minigames.snake

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Haptics(context: Context) {
    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun tick() = vibrate(VibrationEffect.createOneShot(18L, 80))
    fun gold() = vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 40, 25, 40, 25), -1))
    fun death() = vibrate(VibrationEffect.createOneShot(140L, VibrationEffect.DEFAULT_AMPLITUDE))

    private fun vibrate(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(effect)
    }
}
