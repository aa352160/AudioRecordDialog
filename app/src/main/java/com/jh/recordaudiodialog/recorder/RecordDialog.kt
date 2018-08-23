package com.jh.recordaudiodialog.recorder

import android.Manifest
import android.app.Activity
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cafe.adriel.androidaudioconverter.AndroidAudioConverter
import cafe.adriel.androidaudioconverter.callback.IConvertCallback
import cafe.adriel.androidaudioconverter.callback.ILoadCallback
import com.jh.recordaudiodialog.R
import com.jh.recordaudiodialog.R.id.*
import com.jh.recordaudiodialog.dialog.BaseDialog
import com.jh.recordaudiodialog.dialog.BottomUpDialog
import com.jh.recordaudiodialog.toast
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.dialog_record_audio.*
import omrecorder.AudioRecordConfig
import omrecorder.PullTransport
import omrecorder.PullableSource
import java.io.File
import java.lang.Exception

/**
 * Created by jh352160 on 2018/8/8.
 * 音频录制Dialog
 */

class RecordDialog: BottomUpDialog(){
    override fun getTagName() = "recordAudio"

    private val STATE_RECORD_INIT = 1           // 初始状态
    private val STATE_RECORD_RECORDING = 2      // 正在录音
    private val STATE_RECORD_PLAY = 3           // 正在播放录音
    private val STATE_RECORD_PAUSE = 4          // 暂停录音

    private val MAX_RECORD_TIME = 180000

    private var recorder : CustomRecorder? = null
    private val mediaPlayer by lazy { MediaPlayer() }
    private var recordState = STATE_RECORD_INIT
    private var recordTime = 0L  //录制时间，单位为ms

    //录音计时器
    private var recordTimer: CountDownTimer? = null

    //录音播放计时器
    private var recordPlayTimer: CountDownTimer? = null

    var onRecordFinishListener: ((recordFile: File) -> Unit)? = null

    companion object {
        fun newInstance() = RecordDialog()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            LayoutInflater.from(context).inflate(R.layout.dialog_record_audio, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initConvertor()

        isCancelable = true

        fl_record_primary.setOnClickListener {
            if (view_start.visibility == View.VISIBLE) startRecording() else pauseRecording() }
        iv_delete.setOnClickListener { deleteRecording() }
        iv_play.setOnClickListener { playRecoding() }
        tv_cancel.setOnClickListener { dismiss() }
        tv_submit.setOnClickListener { finishRecording() }

        changeState(STATE_RECORD_INIT)
    }

    private fun initConvertor() {
        AndroidAudioConverter.load(context, object : ILoadCallback {
            override fun onSuccess() {}
            override fun onFailure(p0: Exception?) {
                toast("录音组件初始化异常")
                dismiss()
            }
        })
    }

    private fun pauseRecording() {
        changeState(STATE_RECORD_PAUSE)
        recordTimer?.cancel()
        recorder!!.pauseRecording()
    }

    private fun startRecording() {
        if (recordTime >= MAX_RECORD_TIME){
            toast("回答不能超过${MAX_RECORD_TIME / 1000}s")
            return
        }

        isCancelable = false
        if (recordState == STATE_RECORD_INIT) {
            recorder = createRecorder()
            recorder!!.startRecording()
        }else{
            recorder!!.resumeRecording()
        }

        startRecordTimer()
        changeState(STATE_RECORD_RECORDING)
    }

    private fun deleteRecording(){
        isCancelable = true

        changeState(STATE_RECORD_INIT)
        recordTime = 0
        recorder!!.stopRecording()
    }

    private fun playRecoding(){
        if (recordTime >= MAX_RECORD_TIME) {toast("录制时间到达上限"); return}

        if (recordState == STATE_RECORD_PAUSE) {
            mediaPlayer.setOnCompletionListener { playRecoding() }
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.fromFile(recorder!!.tempFile))
            mediaPlayer.prepare()
            mediaPlayer.start()
            startPlayRecordTimer()
            changeState(STATE_RECORD_PLAY)

//            progress_view.maxProgress = 100f
        }else{
            recordPlayTimer?.cancel()
//            progressAnim!!.cancel()
            progress_view.currentProgress = 0f
            mediaPlayer.stop()
            changeState(STATE_RECORD_PAUSE)
        }
    }

    private fun finishRecording() {
        recorder!!.stopRecording()

        //将wav转化为mp3
        AndroidAudioConverter.with(context).setFile(recorder!!.recordFile)
                .setFormat(cafe.adriel.androidaudioconverter.model.AudioFormat.MP3)
                .setCallback(object : IConvertCallback {
                    override fun onSuccess(p0: File?) {
                        onRecordFinishListener?.invoke(p0!!)
                        dismiss()
                    }
                    override fun onFailure(p0: Exception?) {
                        toast("录音组件异常")
                    }
                }).convert()
    }

    /**
     * 根据状态改变控件显示属性
     */
    private fun changeState(recordState: Int){
        when(recordState){
            STATE_RECORD_INIT -> {
                ll_tool.visibility = View.GONE
                iv_play.visibility = View.GONE
                iv_delete.visibility = View.GONE
                view_start.visibility = View.VISIBLE
                view_stop.visibility = View.GONE
                fl_record_primary.isEnabled = true
                tv_hint.text = "点击回答"
                tv_time.text = "回答限时180s"
            }
            STATE_RECORD_RECORDING -> {
                ll_tool.visibility = View.GONE
                iv_play.visibility = View.GONE
                iv_delete.visibility = View.GONE
                view_start.visibility = View.GONE
                view_stop.visibility = View.VISIBLE
                fl_record_primary.isEnabled = true
                tv_hint.text = "点击暂停"
            }
            STATE_RECORD_PAUSE -> {
                ll_tool.visibility = View.VISIBLE
                iv_play.visibility = View.VISIBLE
                iv_delete.visibility = View.VISIBLE
                view_start.visibility = View.VISIBLE
                view_stop.visibility = View.GONE
                fl_record_primary.isEnabled = true
                iv_delete.isEnabled = true
                iv_play.setImageResource(R.drawable.record_audio_play)
                tv_time.text = "${recordTime/60000}:${formatSecondLongToString((recordTime % 60000) / 1000)}"
                tv_hint.text = "继续录制"
            }
            STATE_RECORD_PLAY -> {
                ll_tool.visibility = View.GONE
                iv_play.visibility = View.VISIBLE
                iv_delete.visibility = View.VISIBLE
                view_start.visibility = View.VISIBLE
                view_stop.visibility = View.GONE
                fl_record_primary.isEnabled = false
                iv_delete.isEnabled = false
                iv_play.setImageResource(R.drawable.record_audio_pause)
                tv_hint.text = ""
            }
        }
        this.recordState = recordState
    }

    private fun startRecordTimer(){
        recordTimer = object : CountDownTimer(MAX_RECORD_TIME - recordTime,100){
            override fun onTick(millisUntilFinished: Long) {
                recordTime += 100
                tv_time.text = "${recordTime / 60000}:${formatSecondLongToString((recordTime % 60000) / 1000)}"
            }
            override fun onFinish() {
                pauseRecording()
                toast("录制时间到达上限")
            }
        }.start()
    }

    private fun startPlayRecordTimer(){
        var countDownTime = recordTime.toFloat()
        recordPlayTimer = object : CountDownTimer(recordTime,100){
            init { progress_view.maxProgress = countDownTime }

            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = millisUntilFinished / 1000
                tv_time.text = "${timeLeft/60}:${formatSecondLongToString(timeLeft % 60)}"
                progress_view.currentProgress = countDownTime - millisUntilFinished
            }
            override fun onFinish() {}
        }.start()
    }

    private fun formatSecondLongToString(second: Long) = if (second < 10) "0$second" else "$second"

    private fun createRecorder() = CustomRecorder(PullTransport.Default(mic(),
            PullTransport.OnAudioChunkPulledListener {}),file(),tempFile())

    private fun mic() = PullableSource.Default(AudioRecordConfig.Default(
            MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_IN_MONO, 44100))

    private fun file() = File(context!!.externalCacheDir, "record.wav")
    private fun tempFile() = File(context!!.externalCacheDir, "temp.wav")

    override fun dismiss() {
        super.dismiss()
        // 如果reocrder再daialog关闭前已经停止，则调用stopRecording会抛出IllegalStateException
        try {
            recorder?.stopRecording()
        }catch (e: IllegalStateException){}
    }
}