package com.tvsoft.portfolioanalysis

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class AppExecutors {
    private var mDiskIO: Executor? = null
    private var mNetworkIO: Executor? = null
    private var mMainThread: Executor? = null

    private fun AppExecutors(diskIO: Executor, networkIO: Executor, mainThread: Executor) {
        mDiskIO = diskIO;
        mNetworkIO = networkIO;
        mMainThread = mainThread;
    }

    fun AppExecutors() {
        AppExecutors(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(3),
        MainThreadExecutor())
    }

    fun diskIO(): Executor? {
        return mDiskIO
    }

    fun networkIO(): Executor? {
        return mNetworkIO
    }

    fun mainThread(): Executor? {
        return mMainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }
}