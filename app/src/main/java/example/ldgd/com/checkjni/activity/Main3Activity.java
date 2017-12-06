package example.ldgd.com.checkjni.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import example.ldgd.com.checkjni.R;

public class Main3Activity extends AppCompatActivity implements View.OnClickListener {
    /**
     * 需要申请的权限
     */
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    RecordAudio recordTask;
    private AudioRecord audioRecord;
    PlayAudio playTask;

    Button startRecordingButton, stopRecordingButton, startPlaybackButton,
            stopPlaybackButton;
    TextView statusText;

    File recordingFile;

    boolean isRecording = false;
    boolean isPlaying = false;

    int frequency = 8000;
 //   int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

//    int frequency = 11025;
//    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;

    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main3);

        statusText = (TextView) this.findViewById(R.id.StatusTextView);

        startRecordingButton = (Button) this
                .findViewById(R.id.StartRecordingButton);
        stopRecordingButton = (Button) this
                .findViewById(R.id.StopRecordingButton);
        startPlaybackButton = (Button) this
                .findViewById(R.id.StartPlaybackButton);
        stopPlaybackButton = (Button) this
                .findViewById(R.id.StopPlaybackButton);

        startRecordingButton.setOnClickListener(this);
        stopRecordingButton.setOnClickListener(this);
        startPlaybackButton.setOnClickListener(this);
        stopPlaybackButton.setOnClickListener(this);

        stopRecordingButton.setEnabled(false);
        startPlaybackButton.setEnabled(false);
        stopPlaybackButton.setEnabled(false);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(Main3Activity.this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"没有录音权限",Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(Main3Activity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        } else {
            initFilePath();
        }


/*        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(Main2Activity.this,new String[]{Manifest.permission.RECORD_AUDIO},
//                    1);

            Toast.makeText(this,"没有录音权限",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"you权限",Toast.LENGTH_SHORT).show();
        }*/


    }

    public void onClick(View v) {
        if (v == startRecordingButton) {
            record();
        } else if (v == stopRecordingButton) {
            stopRecording();
        } else if (v == startPlaybackButton) {
            play();
        } else if (v == stopPlaybackButton) {
            stopPlaying();
        }
    }

    public void play() {
        startPlaybackButton.setEnabled(true);

        playTask = new PlayAudio();
        playTask.execute();

        stopPlaybackButton.setEnabled(true);
    }

    public void stopPlaying() {
        isPlaying = false;
        stopPlaybackButton.setEnabled(false);
        startPlaybackButton.setEnabled(true);
    }

    public void record() {
        startRecordingButton.setEnabled(false);
        stopRecordingButton.setEnabled(true);

        // For Fun
        startPlaybackButton.setEnabled(true);

        recordTask = new RecordAudio();
        recordTask.execute();
    }

    public void stopRecording() {
        isRecording = false;
    }

    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            isPlaying = true;

            int bufferSize = AudioTrack.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            byte[] audiodata = new byte[bufferSize];

            try {
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(
                                recordingFile)));

                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);

                audioTrack.play();

                while (isPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < audiodata.length) {
                        audiodata[i] = dis.readByte();
                        i++;
                    }
                    audioTrack.write(audiodata, 0, audiodata.length);
                }

                dis.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startPlaybackButton.setEnabled(false);
                        stopPlaybackButton.setEnabled(true);
                    }
                });

            } catch (Throwable t) {
                Log.e("AudioTrack", "Playback Failed");
            }

            return null;
        }
    }

    private class RecordAudio extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            isRecording = true;

            try {
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(new FileOutputStream(
                                recordingFile)));

                int bufferSize = 4 * AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);


                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();

                int r = 0;
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            bufferSize);
                    for (int i = 0; i < bufferReadResult; i++) {
                        dos.writeByte(buffer[i]);
                    }

                    publishProgress(new Integer(r));
                    r++;
                }

                // 关闭录音
                releaseAudiorecord();
                dos.close();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }

            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            statusText.setText(progress[0].toString());
        }

        protected void onPostExecute(Void result) {
            startRecordingButton.setEnabled(true);
            stopRecordingButton.setEnabled(false);
            startPlaybackButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    initFilePath();
                } else {
                    Toast.makeText(Main3Activity.this, "拒绝权限将无法使用应用程序", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initFilePath() {

        File path = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/Android/data/AudioRecorder/files/");
        path.mkdirs();

        try {
            recordingFile = File.createTempFile("recording", ".pcm", path);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create file on SD card", e);
        }
    }


    private void releaseAudiorecord() {
        // 防止某些手机崩溃，例如联想
        audioRecord.stop();
        // 彻底释放资源
        audioRecord.release();
        audioRecord = null;
    }
}
