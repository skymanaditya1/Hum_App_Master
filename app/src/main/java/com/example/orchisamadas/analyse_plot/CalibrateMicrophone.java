package com.example.orchisamadas.analyse_plot;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This activity calibrates an android device's microphone.
 * It makes a recording using 1. An external microphone.
 * 2. Using the android's microphone
 * After converting the data from time domain into frequency domain,
 * the gain for each frequency band is calculated.
 */

public class CalibrateMicrophone extends AppCompatActivity {

    final int SAMPLING_RATE = 8000, CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_DEFAULT,
            AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT, RECORDING_DURATION = 5;
    final String EXTERNAL_AUDIO_FILENAME = "external_audio.wav", ANDROID_AUDIO_FILENAME = "android_audio.wav";
    AudioRecord recorder;
    Button startCalibration, startRecording, startPlayback;
    TextView timer;
    CounterClass counterClass = new CounterClass(RECORDING_DURATION * 1000, 1000);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate_microphone);

        startCalibration = (Button) findViewById(R.id.button_calibrate_microphone);
        startRecording = (Button) findViewById(R.id.button_start_recording);
        startPlayback = (Button) findViewById(R.id.button_start_playback);
        timer = (TextView) findViewById(R.id.textView_timer);

        startCalibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Please insert an external microphone to start recording
                externalMicrophoneRecording();
                // androidMicrophoneRecording();
            }
        });
    }

    public class CounterClass extends CountDownTimer{
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            String hms = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
            timer.setText("Time remaining : " + hms);
        }

        @Override
        public void onFinish() {
            timer.setText("Captured Sound");
        }
    }

    public void externalMicrophoneRecording(){
        Toast.makeText(CalibrateMicrophone.this,
                "Please insert an external microphone to continue", Toast.LENGTH_SHORT).show();
        startRecording.setVisibility(View.VISIBLE);
        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CaptureAudio().execute();
            }
        });
    }

    public class CaptureAudio extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_ENCODING);
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLING_RATE,
                    CHANNEL_CONFIG, AUDIO_ENCODING, bufferSize);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(CalibrateMicrophone.this, "Recording device initialization failed",
                        Toast.LENGTH_SHORT).show();
                recorder.release();
                recorder=null;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            int samplesRead = 0;
            int sampleBufferLength = nearestPow2Length(SAMPLING_RATE * RECORDING_DURATION);
            short[] sampleBuffer = new short[sampleBufferLength];
            counterClass.start();
            recorder.startRecording();
            while (samplesRead < sampleBufferLength)
                samplesRead += recorder.read(sampleBuffer, samplesRead, sampleBufferLength - samplesRead);
            if (recorder != null) {recorder.release(); recorder=null;}
            saveRecording(sampleBuffer, EXTERNAL_AUDIO_FILENAME);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            startPlayback.setVisibility(View.VISIBLE);
            startPlayback.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playbackRecording(EXTERNAL_AUDIO_FILENAME);
                }
            });
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    public int nearestPow2Length(int length){
        int temp = (int) (Math.log(length) / Math.log(2.0) + 0.5); length = 1;
        for (int n=1; n<=temp; n++) length*=2;
        return length;
    }

    public void androidMicrophoneRecording(){

    }

    // Method for saving the recorded file into the phone's memory
    public void saveRecording(short[] sampleBuffer, String filename){
        /**File file = new File(Environment.getExternalStorageDirectory(),
                new SimpleDateFormat("yyyyMMddhhmmss'.wav'").format(new Date()));*/
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        if (file.exists()) file.delete();
        try {
            file.createNewFile();
            DataOutputStream dataOutputStream =
                    new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            for(int n=0; n<sampleBuffer.length; n++) dataOutputStream.writeShort(sampleBuffer[n]);
        } catch (IOException e) {
            System.out.println("Exception while saving record of type : " + e.toString());
        }
    }

    // Method for playing back the recorded audio
    public void playbackRecording(String filename){
        File file = new File(Environment.getExternalStorageDirectory(), filename);
        int audioLength = (int)file.length()/2;
        short[] audio = new short[audioLength];
        try {
            DataInputStream dataInputStream =
                    new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            int audioRead = 0;
            while (dataInputStream.available() > 0 ){
                audio[audioRead++] = dataInputStream.readShort();
            }
        } catch (IOException e) {
            System.out.println("Exception while playback record of type : " + e.toString());
        }

        // Create an AudioTrack object for playback
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE,
                CHANNEL_CONFIG, AUDIO_ENCODING, audioLength, AudioTrack.MODE_STREAM);
        audioTrack.play();
        audioTrack.write(audio, 0, audioLength);
    }
}