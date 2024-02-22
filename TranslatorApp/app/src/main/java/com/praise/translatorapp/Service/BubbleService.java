package com.praise.translatorapp.Service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.praise.translatorapp.R;

import java.util.Locale;

public class BubbleService extends Service {


    boolean isShown;
    int LAYOUT_FLAG;
    ViewGroup mFLoatingView;
    WindowManager windowManager;
    CardView floatingBubble, floatingDialog;
    float height, width;
    TextView sourceLangLabel,targetLangLabel, sourceLangTv, targetLangTv,targetOutputTv;
    EditText sourceLangEdt;
    ImageView switchLangBtn, translateBtn, readSourceBtn, clearSourceBtn, copyBtn, readTargetBtn,closeBtn;
    boolean isEnglishToChinese = true;
    TextToSpeech textToSpeech;
    private int CLICK_ACTION_THRESHOLD = 200;
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isShown = false;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        mFLoatingView = (ViewGroup) inflater.inflate(R.layout.floating_window, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        //initial position of the floating bubble...
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.x = 0;
        layoutParams.y = 0;

        //for the close button...
        WindowManager.LayoutParams imageParam = new WindowManager.LayoutParams(140, 140, LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,

                PixelFormat.TRANSLUCENT);
        imageParam.gravity = Gravity.BOTTOM | Gravity.CENTER;
        imageParam.y = 100;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        closeBtn = new ImageView(this);
        closeBtn.setImageResource(R.drawable.close_button_icon);
        closeBtn.setVisibility(View.INVISIBLE);
        mFLoatingView.setVisibility(View.VISIBLE);
        height =windowManager.getDefaultDisplay().getHeight();
        width = windowManager.getDefaultDisplay().getWidth();
        floatingBubble = (CardView) mFLoatingView.findViewById(R.id.bubble);
        floatingDialog = (CardView) mFLoatingView.findViewById(R.id.dialod_box);
        initialize();
        validateTranslator();
        windowManager.addView(closeBtn, imageParam);
        windowManager.addView(mFLoatingView, layoutParams);
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
        sourceLangEdt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams floatWindowLayoutParamUpdateFlag = layoutParams;
                floatWindowLayoutParamUpdateFlag.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                windowManager.updateViewLayout(mFLoatingView, floatWindowLayoutParamUpdateFlag);
                return false;
            }
        });

        floatingBubble.setOnTouchListener(new View.OnTouchListener() {
            int initialX, initialY;
            float initialTouchX, initialTouchY;
            float endTouchX, endTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        closeBtn.setVisibility(View.VISIBLE);
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return  true;
                    case MotionEvent.ACTION_UP:
                        closeBtn.setVisibility(View.GONE);
                        layoutParams.x = initialX + (int) (initialTouchX - event.getRawX());
                        layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        endTouchY= event.getRawY();
                        endTouchX = event.getRawX();
                        if(layoutParams.y > (height* 0.7)){
                            stopSelf();
                        }
                        if(isAclick(initialTouchX,endTouchX, initialTouchY, endTouchY)){
                            if(isShown==true){
                                floatingDialog.setVisibility(View.GONE);
                                isShown = false;
                                WindowManager.LayoutParams floatWindowLayoutParamUpdateFlag = layoutParams;
                                floatWindowLayoutParamUpdateFlag.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                                windowManager.updateViewLayout(mFLoatingView, floatWindowLayoutParamUpdateFlag);
                            }
                            else {
                                floatingDialog.setVisibility(View.VISIBLE);
                                isShown = true;
                            }
                        }
                        return true;
                        case MotionEvent.ACTION_MOVE:
                            layoutParams.x = initialX + (int) (initialTouchX - event.getRawX());
                            layoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(mFLoatingView, layoutParams);
                            if (layoutParams.y > (height * 0.7)) {
                                closeBtn.setImageResource(R.drawable.close_button_icon);

                            } else {
                                closeBtn.setImageResource(R.drawable.smile);
                            }
                            return true;

                }

                return false;
            }
        });



        return START_STICKY;
    }

    /**
     * checking if floating bubble has been clicked...
     */
    private boolean isAclick(float startX, float endX , float startY , float endY){
        float differenceX = Math.abs(startX - endX);
        float differenceY = Math.abs(startY - endY);
        return !(differenceX > CLICK_ACTION_THRESHOLD/* =5 */ || differenceY > CLICK_ACTION_THRESHOLD);

    }

    /**
     * initializing variables...
     */
    private void initialize(){
        sourceLangLabel = mFLoatingView.findViewById(R.id.sourceLang);
        targetLangLabel = mFLoatingView.findViewById(R.id.targetLang);
        sourceLangTv = mFLoatingView.findViewById(R.id.sourceLangTv);
        targetLangTv = mFLoatingView.findViewById(R.id.targetLangTv);
        targetOutputTv = mFLoatingView.findViewById(R.id.targetOutputTV);
        sourceLangEdt = mFLoatingView.findViewById(R.id.sourceLangEDT);
        switchLangBtn = mFLoatingView.findViewById(R.id.switchLangBtn);
        translateBtn = mFLoatingView.findViewById(R.id.translateBtn);
        readSourceBtn = mFLoatingView.findViewById(R.id.readSourceBtn);
        readTargetBtn = mFLoatingView.findViewById(R.id.readtargetBtn);
        clearSourceBtn = mFLoatingView.findViewById(R.id.clearSourceBtn);
        copyBtn = mFLoatingView.findViewById(R.id.copyText);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US);
                }
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
                public void onFailure(@NonNull Exception e) {

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mFLoatingView != null){
            windowManager.removeView(mFLoatingView);
        }
        if(closeBtn != null){
            windowManager.removeView(closeBtn);
        }
    }
}
