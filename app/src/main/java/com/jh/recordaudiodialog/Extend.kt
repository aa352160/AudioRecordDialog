package com.jh.recordaudiodialog

import android.support.v4.app.Fragment
import android.widget.Toast

/**
 * Created by jh352160 on 2018/8/9.
 */

fun Fragment.toast(msg: String){
    Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
}