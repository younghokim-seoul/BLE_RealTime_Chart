package com.cm.rxandroidble.util

import android.widget.Toast
import com.cm.rxandroidble.MyApplication

class Util {
    companion object{
        fun showNotification(msg: String){
            Toast.makeText(MyApplication.applicationContext(),msg,Toast.LENGTH_SHORT).show()
        }
    }
}