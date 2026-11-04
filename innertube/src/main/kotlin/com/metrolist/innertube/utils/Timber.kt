package com.metrolist.innertube.utils

/**
 * Lightweight logger shim for Innertube in JVM/Desktop environments.
 */
object Timber {
    @JvmStatic fun d(message: String?) {}
    @JvmStatic fun d(t: Throwable?, message: String?) {}
    @JvmStatic fun d(t: Throwable?) {}
    @JvmStatic fun i(message: String?) {}
    @JvmStatic fun w(message: String?) {}
    @JvmStatic fun w(t: Throwable?, message: String?) {}
    @JvmStatic fun w(t: Throwable?) {}
    @JvmStatic fun e(message: String?) {}
    @JvmStatic fun e(t: Throwable?, message: String?) {}
    @JvmStatic fun e(t: Throwable?) {}
    @JvmStatic fun tag(tag: String): Timber = this
}
