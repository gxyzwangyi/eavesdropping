package fr.pchab.androidrtc.desperated;

/**
 * Created by wangyi on 17/3/23.
 */


import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect.Descriptor;
import android.os.Process;
import android.util.Log;

import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.nio.ByteBuffer;



public class WebRtcAudioRecord {
    private static final boolean DEBUG = false;
    private static final String TAG = "WebRtcAudioRecord";
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int BYTES_PER_FRAME = 2;
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;
    private static final int BUFFERS_PER_SECOND = 100;
    private ByteBuffer byteBuffer;
    private final int bytesPerBuffer;
    private final int framesPerBuffer;
    private final int sampleRate;
    private final long nativeAudioRecord;
    private final AudioManager audioManager;
    private final Context context;
    private AudioRecord audioRecord = null;
    private  WebRtcAudioRecord.AudioRecordThread audioThread = null;
    private AcousticEchoCanceler aec = null;
    private boolean useBuiltInAEC = false;

    WebRtcAudioRecord(Context context, long nativeAudioRecord) {
        Logd("ctor" + WebRtcAudioUtils.getThreadInfo());
        this.context = context;
        this.nativeAudioRecord = nativeAudioRecord;
        this.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        this.sampleRate = this.GetNativeSampleRate();
        this.bytesPerBuffer = 2 * (this.sampleRate / 100);
        this.framesPerBuffer = this.sampleRate / 100;
        ByteBuffer var10001 = this.byteBuffer;
        this.byteBuffer = ByteBuffer.allocateDirect(this.bytesPerBuffer);
        Logd("byteBuffer.capacity: " + this.byteBuffer.capacity());
        this.nativeCacheDirectBufferAddress(this.byteBuffer, nativeAudioRecord);
    }

    public int GetNativeSampleRate() {
        return WebRtcAudioUtils.GetNativeSampleRate(this.audioManager);
    }

    public static boolean BuiltInAECIsAvailable() {
        return !WebRtcAudioUtils.runningOnJellyBeanOrHigher()?false:AcousticEchoCanceler.isAvailable();
    }

    public boolean EnableBuiltInAEC(boolean enable) {
        Logd("EnableBuiltInAEC(" + enable + ')');
        if(!WebRtcAudioUtils.runningOnJellyBeanOrHigher()) {
            return false;
        } else {
            this.useBuiltInAEC = enable;
            if(this.aec != null) {
                int ret = this.aec.setEnabled(enable);
                if(ret != 0) {
                    Loge("AcousticEchoCanceler.setEnabled failed");
                    return false;
                }

                Logd("AcousticEchoCanceler.getEnabled: " + this.aec.getEnabled());
            }

            return true;
        }
    }

    public int InitRecording(int sampleRate) {
        Logd("InitRecording(sampleRate=" + sampleRate + ")");
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, 16, 2);
        Logd("AudioRecord.getMinBufferSize: " + minBufferSize);
        if(this.aec != null) {
            this.aec.release();
            this.aec = null;
        }

        assertTrue(this.audioRecord == null);
        int bufferSizeInBytes = Math.max(this.byteBuffer.capacity(), minBufferSize);
        Logd("bufferSizeInBytes: " + bufferSizeInBytes);

        try {
            this.audioRecord = new AudioRecord(7, sampleRate, 16, 2, bufferSizeInBytes);
        } catch (IllegalArgumentException var6) {
            Logd(var6.getMessage());
            return -1;
        }

        assertTrue(this.audioRecord.getState() == 1);
        Logd("AudioRecord session ID: " + this.audioRecord.getAudioSessionId() + ", " + "audio format: " + this.audioRecord.getAudioFormat() + ", " + "channels: " + this.audioRecord.getChannelCount() + ", " + "sample rate: " + this.audioRecord.getSampleRate());
        Logd("AcousticEchoCanceler.isAvailable: " + BuiltInAECIsAvailable());
        if(!BuiltInAECIsAvailable()) {
            return this.framesPerBuffer;
        } else {
            this.aec = AcousticEchoCanceler.create(this.audioRecord.getAudioSessionId());
            if(this.aec == null) {
                Loge("AcousticEchoCanceler.create failed");
                return -1;
            } else {
                int ret = this.aec.setEnabled(this.useBuiltInAEC);
                if(ret != 0) {
                    Loge("AcousticEchoCanceler.setEnabled failed");
                    return -1;
                } else {
                    Descriptor descriptor = this.aec.getDescriptor();
                    Logd("AcousticEchoCanceler name: " + descriptor.name + ", " + "implementor: " + descriptor.implementor + ", " + "uuid: " + descriptor.uuid);
                    Logd("AcousticEchoCanceler.getEnabled: " + this.aec.getEnabled());
                    return this.framesPerBuffer;
                }
            }
        }
    }

    public boolean StartRecording() {
        Logd("StartRecording");
        assertTrue(this.audioRecord != null);
        assertTrue(this.audioThread == null);
        this.audioThread = new  WebRtcAudioRecord.AudioRecordThread("AudioRecordJavaThread");
        this.audioThread.start();
        return true;
    }

    public boolean StopRecording() {
        Logd("StopRecording");
        assertTrue(this.audioThread != null);
        this.audioThread.joinThread();
        this.audioThread = null;
        if(this.aec != null) {
            this.aec.release();
            this.aec = null;
        }

        this.audioRecord.release();
        this.audioRecord = null;
        return true;
    }

    private static void assertTrue(boolean condition) {
        if(!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    private static void Logd(String msg) {
        Log.d("WebRtcAudioRecord", msg);
    }

    private static void Loge(String msg) {
        Log.e("WebRtcAudioRecord", msg);
    }

    private native void nativeCacheDirectBufferAddress(ByteBuffer var1, long var2);

    private native void nativeDataIsRecorded(int var1, long var2);

    private class AudioRecordThread extends Thread {
        private volatile boolean keepAlive = true;

        public AudioRecordThread(String name) {
            super(name);
        }

        public void run() {
            Process.setThreadPriority(-19);
            WebRtcAudioRecord.Logd("AudioRecordThread" + WebRtcAudioUtils.getThreadInfo());

            try {
                WebRtcAudioRecord.this.audioRecord.startRecording();
            } catch (IllegalStateException var5) {
                WebRtcAudioRecord.Loge("AudioRecord.startRecording failed: " + var5.getMessage());
                return;
            }

            WebRtcAudioRecord.assertTrue(WebRtcAudioRecord.this.audioRecord.getRecordingState() == 3);
            long lastTime = System.nanoTime();

            while(this.keepAlive) {
                int e = WebRtcAudioRecord.this.audioRecord.read(WebRtcAudioRecord.this.byteBuffer, WebRtcAudioRecord.this.byteBuffer.capacity());
                if(e == WebRtcAudioRecord.this.byteBuffer.capacity()) {
                    WebRtcAudioRecord.this.nativeDataIsRecorded(e, WebRtcAudioRecord.this.nativeAudioRecord);
                } else {
                    WebRtcAudioRecord.Loge("AudioRecord.read failed: " + e);
                    if(e == -3) {
                        this.keepAlive = false;
                    }
                }
            }

            try {
                WebRtcAudioRecord.this.audioRecord.stop();
            } catch (IllegalStateException var4) {
                WebRtcAudioRecord.Loge("AudioRecord.stop failed: " + var4.getMessage());
            }

        }

        public void joinThread() {
            this.keepAlive = false;

            while(this.isAlive()) {
                try {
                    this.join();
                } catch (InterruptedException var2) {
                    ;
                }
            }

        }
    }
}
