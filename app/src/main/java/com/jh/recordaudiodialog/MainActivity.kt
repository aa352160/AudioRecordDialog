package com.jh.recordaudiodialog

import android.Manifest
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.widget.Toast
import com.jh.recordaudiodialog.recorder.RecordDialog
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_action.setOnClickListener {
            RxPermissions(this).request(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe { isGrand ->
                        if (isGrand) {
                            RecordDialog.newInstance().apply {
                                onRecordFinishListener = {
                                    Toast.makeText(this@MainActivity,"录音文件路径为：${it.absolutePath}",Toast.LENGTH_LONG).show()
                                }
                            }.show(this)
                        }else{
                            Toast.makeText(this,"缺少录音必须的权限",Toast.LENGTH_LONG).show()
                        }
                    }
        }
    }
}
