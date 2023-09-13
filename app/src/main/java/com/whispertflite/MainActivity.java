package com.whispertflite;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.whispertflite.utils.WaveUtil;
import com.whispertflite.asr.Recorder;
import com.whispertflite.asr.Whisper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private Whisper mWhisper = null;
    private Recorder mRecorder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String[] waveFileName = {null};
        final Handler handler = new Handler(Looper.getMainLooper());

        TextView tvResult = findViewById(R.id.tvResult);

        // Implementation of record button functionality
        Button btnMicRec = findViewById(R.id.btnRecord);
        btnMicRec.setOnClickListener(v -> {
            if (mRecorder != null && mRecorder.isInProgress()) {
                Log.d(TAG, "Recording is in progress... stopping...");
                stopRecording();
            } else {
                Log.d(TAG, "Start recording...");
                startRecording();
            }
        });

        // Implementation of transcribe button functionality
        Button btnTranscb = findViewById(R.id.btnTranscb);
        btnTranscb.setOnClickListener(v -> {
            if (mRecorder != null && mRecorder.isInProgress()) {
                Log.d(TAG, "Recording is in progress... stopping...");
                stopRecording();
            }

            if (mWhisper != null && mWhisper.isInProgress()) {
                Log.d(TAG, "Whisper is already in progress...!");
                stopTranscription();
            } else {
                Log.d(TAG, "Start transcription...");
                String waveFilePath = getFilePath(waveFileName[0]);
                startTranscription(waveFilePath);
            }
        });

        // Implementation of translate button functionality
        Button btnTransl = findViewById(R.id.btnTransl);
        btnTransl.setOnClickListener(v -> {
            if (mRecorder != null && mRecorder.isInProgress()) {
                Log.d(TAG, "Recording is in progress... stopping...");
                stopRecording();
            }

            if (mWhisper != null && mWhisper.isInProgress()) {
                Log.d(TAG, "Whisper is already in progress...!");
                stopTranslation();
            } else {
                Log.d(TAG, "Start translation...");
                String waveFilePath = getFilePath(waveFileName[0]);
                startTranslation(waveFilePath);
            }
        });

        // Implementation of file spinner functionality
        ArrayList<String> files = new ArrayList<>();
        // files.add(WaveUtil.RECORDING_FILE);
        try {
            String[] assetFiles = getAssets().list("");
            for (String file : assetFiles) {
                if (file.endsWith(".wav"))
                    files.add(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] fileArray = new String[files.size()];
        files.toArray(fileArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fileArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = findViewById(R.id.spnrFiles);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                waveFileName[0] = fileArray[position];

                if (waveFileName[0].equals(WaveUtil.RECORDING_FILE))
                    btnMicRec.setVisibility(View.VISIBLE);
                else
                    btnMicRec.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // English-only model and vocab
//        boolean isMultilingual = false;
//        String modelPath = getFilePath("whisper-tiny-en.tflite");
//        String vocabPath = getFilePath("filters_vocab_gen.bin");

        // Multilingual model and vocab
        boolean isMultilingual = true;
//        String modelPath = getFilePath("whisper-tiny.tflite"); // Always Translate, ES -> EN
        String modelPath = getFilePath("whisper-base.tflite"); // Always Transcribe ES -> EN
        String vocabPath = getFilePath("filters_vocab_multilingual.bin");

        // TODO: pass model and vocab as per requirement
        mWhisper = new Whisper(this, modelPath, vocabPath, isMultilingual);
        mWhisper.setUpdateListener(message -> {
            if (message.equals(Whisper.MSG_LOADING_MODEL)) {
                // write code as per need
            } else if (message.equals(Whisper.MSG_PROCESSING)) {
                // write code as per need
            } else if (message.equals(Whisper.MSG_FILE_NOT_FOUND)) {
                // write code as per need
            }

            handler.post(() -> tvResult.setText(message));
        });

        mRecorder = new Recorder(this);
        mRecorder.setUpdateListener(message -> {
            if (message.equals(Recorder.MSG_RECORDING_STARTED)) {
                handler.post(() -> btnMicRec.setText(Recorder.ACTION_STOP));
            } else if (message.equals(Recorder.MSG_RECORDING_COMPLETED)) {
                handler.post(() -> btnMicRec.setText(Recorder.ACTION_RECORD));
            } else if (message.equals(Recorder.MSG_RECORDING)) {
                // write code as per requirement
            }

            handler.post(() -> tvResult.setText(message));
        });

        // Call the method to copy specific file types from assets to data folder
        String[] extensionsToCopy = {"pcm", "bin", "wav", "tflite"};
        copyAssetsWithExtensionsToDataFolder(this, extensionsToCopy);

        // Assume this Activity is the current activity, check record permission
        checkRecordPermission();

        // for debugging
//        testParallelProcessing();
    }

    private void checkRecordPermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Record permission is granted");
        } else {
            Log.d(TAG, "Requesting record permission");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Log.d(TAG, "Record permission is granted");
        else
            Log.d(TAG, "Record permission is not granted");
    }

    // Recording calls
    private void startRecording() {
        checkRecordPermission();

        String waveFilePath = getFilePath(WaveUtil.RECORDING_FILE);
        mRecorder.setFilePath(waveFilePath);
        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
    }

    // Transcription calls
    private void startTranscription(String waveFilePath) {
        mWhisper.setFilePath(waveFilePath);
        mWhisper.setAction(Whisper.ACTION_TRANSCRIBE_JAVA);
        mWhisper.start();
    }

    private void stopTranscription() {
        mWhisper.stop();
    }

    // Translation calls
    private void startTranslation(String waveFilePath) {
        mWhisper.setFilePath(waveFilePath);
        mWhisper.setAction(Whisper.ACTION_TRANSLATE);
        mWhisper.start();
    }

    private void stopTranslation() {
        mWhisper.stop();
    }

    // Copy assets to data folder
    private static void copyAssetsWithExtensionsToDataFolder(Context context, String[] extensions) {
        AssetManager assetManager = context.getAssets();
        try {
            // Specify the destination directory in the app's data folder
            String destFolder = context.getFilesDir().getAbsolutePath();

            for (String extension : extensions) {
                // List all files in the assets folder with the specified extension
                String[] assetFiles = assetManager.list("");
                for (String assetFileName : assetFiles) {
                    if (assetFileName.endsWith("." + extension)) {
                        File outFile = new File(destFolder, assetFileName);
                        if (outFile.exists())
                            continue;

                        InputStream inputStream = assetManager.open(assetFileName);
                        OutputStream outputStream = new FileOutputStream(outFile);

                        // Copy the file from assets to the data folder
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }

                        inputStream.close();
                        outputStream.flush();
                        outputStream.close();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns file path from data folder
    private String getFilePath(String assetName) {
        File outfile = new File(getFilesDir(), assetName);
        if (!outfile.exists()) {
            Log.d(TAG, "File not found - " + outfile.getAbsolutePath());
        }

        Log.d(TAG, "Returned asset path: " + outfile.getAbsolutePath());
        return outfile.getAbsolutePath();
    }

    // Test code for parallel processing
    private void testParallelProcessing() {

        // Define the file names in an array
        String[] fileNames = {
                "english_test1.wav",
                "english_test2.wav",
                "english_test_3_bili.wav"
        };

        // Multilingual model and vocab
        String modelMultilingual = getFilePath("whisper-tiny.tflite");
        String vocabMultilingual = getFilePath("filters_vocab_multilingual.bin");

        // Perform task for multiple audio files using multilingual model
        for (String fileName : fileNames) {
            Whisper whisper = new Whisper(this, modelMultilingual, vocabMultilingual, true);
            whisper.setUpdateListener(message -> Log.d(TAG, message));
            String waveFilePath = getFilePath(fileName);
            whisper.setFilePath(waveFilePath);
            whisper.start();
        }

        // English-only model and vocab
        String modelEnglish = getFilePath("whisper-tiny-en.tflite");
        String vocabEnglish = getFilePath("filters_vocab_gen.bin");

        // Perform task for multiple audio files using english only model
        for (String fileName : fileNames) {
            Whisper whisper = new Whisper(this, modelEnglish, vocabEnglish, false);
            whisper.setUpdateListener(message -> Log.d(TAG, message));
            String waveFilePath = getFilePath(fileName);
            whisper.setFilePath(waveFilePath);
            whisper.start();
        }
    }
}