package com.praise.translatorapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.praise.translatorapp.Service.BubbleService;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView sourceLangLabel,targetLangLabel, sourceLangTv, targetLangTv,targetOutputTv;
    EditText sourceLangEdt;
    ImageView switchLangBtn, translateBtn, readSourceBtn, clearSourceBtn, copyBtn, readTargetBtn;
    boolean isEnglishToChinese = true;
    TextToSpeech textToSpeech;

    TranslatorOptions option =
            new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.CHINESE).build();
    final Translator englishChineseTranslator =
            Translation.getClient(option);

    TranslatorOptions optionTwo =
            new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.CHINESE)
                    .setTargetLanguage(TranslateLanguage.ENGLISH).build();
    final Translator chineseEnglishTranslator =
            Translation.getClient(optionTwo);
    private  AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        validateTranslator();
        checkOverlayDisplayPermission();
        if(isMyServiceRunning()){
            stopRunningService();
        }
        switchLangBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isEnglishToChinese){
                    setChineseToEnglish();
                    isEnglishToChinese= false;
                }
                else {
                    setEnglishToChinese();
                    isEnglishToChinese = true;
                }
            }
        });
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("copied", targetOutputTv.getText().toString().trim());
                clipboardManager.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "text copied", Toast.LENGTH_SHORT).show();
            }
        });
        clearSourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            sourceLangEdt.getText().clear();
            }
        });
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performTranslation();
            }
        });
        readTargetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              String text=  targetOutputTv.getText().toString().trim();
              textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);

            }
        });
        readSourceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text=  sourceLangEdt.getText().toString().trim();
                textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
            }
        });
        sourceLangEdt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.toString().trim().length() == 0){
                    targetOutputTv.setText("");
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            if(s.toString().endsWith("")){
                performTranslation();
            }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


    }

    private void performTranslation() {
        String sourceText = sourceLangEdt.getText().toString().trim();
        if(isEnglishToChinese){
            englishChineseTranslator.translate(sourceText)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            targetOutputTv.setText(s.trim());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull  Exception e) {

                }
            });

        }
        else {
            chineseEnglishTranslator.translate(sourceText)
                    .addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            targetOutputTv.setText(s.trim());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull  Exception e) {

                }
            });
        }

    }

    private void validateTranslator() {

        // to download offline data for translator
        //you can change to your desired language

        TranslatorOptions options =
                new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.CHINESE)
                        .setTargetLanguage(TranslateLanguage.ENGLISH).build();
        final Translator englishChineseTranslator =
                Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        englishChineseTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
                            public void onSuccess(Object o) {
                                Toast.makeText(getApplicationContext(), "Ready to use", Toast.LENGTH_SHORT).show();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                                Toast.makeText(getApplicationContext(), "Check your internet connection", Toast.LENGTH_SHORT).show();
                            }
                        });

    }

    private void setEnglishToChinese() {
        sourceLangTv.setText("English");
        sourceLangLabel.setText("EN");
        targetLangLabel.setText("CN");
        targetLangTv.setText("Chinese");
    }

    private void setChineseToEnglish() {
        sourceLangTv.setText("Chinese");
        sourceLangLabel.setText("CN");
        targetLangLabel.setText("EN");
        targetLangTv.setText("English");
    }

    /**
     * initialize variables...
     */
    private void initialize() {
        sourceLangLabel = findViewById(R.id.sourceLang);
        targetLangLabel = findViewById(R.id.targetLang);
        sourceLangTv = findViewById(R.id.sourceLangTv);
        targetLangTv = findViewById(R.id.targetLangTv);
        targetOutputTv = findViewById(R.id.targetOutputTV);
        sourceLangEdt = findViewById(R.id.sourceLangEDT);
        switchLangBtn = findViewById(R.id.switchLangBtn);
        translateBtn = findViewById(R.id.translateBtn);
        readSourceBtn = findViewById(R.id.readSourceBtn);
        readTargetBtn = findViewById(R.id.readtargetBtn);
        clearSourceBtn = findViewById(R.id.clearSourceBtn);
        copyBtn = findViewById(R.id.copyText);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
    }

    /**
     * Request permissions....
     */
        private void requestOverlayDisplayPermission(){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle("Screen Overlay Permission Needed");
            builder.setMessage("Enable 'Display over other apps' from settings");
            builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent,RESULT_OK);
                }
            });
            dialog =builder.create();
            dialog.show();
        }
    /**
     * Check if permission was accepted...
     */
    private boolean checkOverlayDisplayPermission(){
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            if(!Settings.canDrawOverlays(this)){
                return false;

            }else {
                return true;
            }
        }
        else {
            return  true;
        }
    }

    /**
     * check if service is already running...
     */
    private boolean isMyServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if(BubbleService.class.getName().equals(serviceInfo.service.getClassName())){
                return  true;
            }
        }
        return  false;
    }

    /**
     * stops running service...
     */
    private void stopRunningService(){
        stopService(new Intent(getApplicationContext(), BubbleService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (checkOverlayDisplayPermission()){
            startService(new Intent(MainActivity.this, BubbleService.class));
            finish();
        }
        else {
            requestOverlayDisplayPermission();
        }
    }
}