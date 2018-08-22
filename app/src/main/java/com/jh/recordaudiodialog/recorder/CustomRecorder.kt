package com.jh.recordaudiodialog.recorder

import com.jh.recordaudiodialog.FileUtil

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files

import omrecorder.AbstractRecorder
import omrecorder.PullTransport

/**
 * Created by jh352160 on 2018/8/9.
 */
class CustomRecorder(pullTransport: PullTransport, var recordFile: File, val tempFile: File) : AbstractRecorder(pullTransport, recordFile) {
    override fun pauseRecording() {
        super.pauseRecording()
        try {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            //            Files.copy(file.toPath(),tempFile.toPath());
            FileUtil.copyFile(recordFile.path, tempFile.path)
            writeWavHeader(tempFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun stopRecording() {
        try {
            super.stopRecording()
            writeWavHeader(recordFile)
        } catch (e: IOException) {
            throw RuntimeException("Error in applying wav header", e)
        }
    }

    @Throws(IOException::class)
    private fun writeWavHeader(targetFile: File) {
        val wavFile = randomAccessFile(targetFile)
        wavFile.seek(0) // to the beginning
        wavFile.write(WavHeader(pullTransport.pullableSource(), targetFile.length()).toBytes())
        wavFile.close()
    }

    private fun randomAccessFile(file: File): RandomAccessFile {
        val randomAccessFile: RandomAccessFile
        try {
            randomAccessFile = RandomAccessFile(file, "rw")
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        }
        return randomAccessFile
    }
}
