package com.abhijith.frpoc.database

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore
import io.objectbox.android.Admin
import io.objectbox.android.AndroidObjectBrowser

object ObjectBoxStore {

    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder().androidContext(context).build()

        val started = Admin(store).start(context);
        Log.i("ObjectBoxAdmin", "Started: $started");

//        if (BuildConfig.DEBUG) {
//            AndroidObjectBrowser(store).start(context)
//        }

    }


}
