package com.example.mis_rfdtester;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;

public class CommentActivity extends AppCompatActivity {

    EditText comment;
    Button cancel;
    Button submit;
    RadioGroup radio;

    StorageUtils su;

    private ArrayList<Float> AccX;
    private ArrayList<Float> AccY;
    private ArrayList<Float> AccZ;
    private ArrayList<Float> RotX;
    private ArrayList<Float> RotY;
    private ArrayList<Float> RotZ;

    RadioGroup.OnCheckedChangeListener listener;

    private boolean true_positive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        comment = findViewById(R.id.Comment);
        cancel = findViewById(R.id.Cancel);
        submit = findViewById(R.id.Submit);
        submit.setEnabled(false);
        radio = findViewById(R.id.radioGroup);

        listener = (new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if(group.getCheckedRadioButtonId() != -1) {
                    submit.setEnabled(true);
                    true_positive = (checkedId == R.id.TP);
                }
            }
        });

        radio.setOnCheckedChangeListener(listener);

        Intent intent = getIntent();

        AccX = (ArrayList<Float>) intent.getSerializableExtra("AccX");
        AccY = (ArrayList<Float>) intent.getSerializableExtra("AccY");
        AccZ = (ArrayList<Float>) intent.getSerializableExtra("AccZ");
        RotX = (ArrayList<Float>) intent.getSerializableExtra("RotX");
        RotY = (ArrayList<Float>) intent.getSerializableExtra("RotY");
        RotZ = (ArrayList<Float>) intent.getSerializableExtra("RotZ");

        su = new StorageUtils();

        if (ContextCompat.checkSelfPermission(CommentActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(CommentActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
    }

    public void submitButtonClicked(View view){
        String commentText = comment.getText().toString();
        Timestamp ts = new Timestamp(System.currentTimeMillis());

        registerComment(commentText, ts);
        registerMotionData(ts);

        returnToMain();
    }

    private void registerComment(String commentText, Timestamp ts){
        File commentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/MotionData", "Comments.CSV");
        if(!commentFile.exists()) {
            su.createFile(commentFile);
            su.addFileHeader(commentFile, new String[]{"Timestamp", "Comment", "Result"});
        }
        su.writeCommentData(commentFile, ts, commentText, true_positive);
    }

    private void registerMotionData(Timestamp ts){
        File motionFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Documents/MotionData", "CommentMotionData.CSV");
        if(!motionFile.exists()) {
            su.createFile(motionFile);
            su.addFileHeader(motionFile, new String[]{"Timestamp", "AccX", "AccY", "AccZ", "RotX", "RotY", "RotZ"});
        }
        su.writeMotionData(motionFile, ts, AccX, AccY, AccZ, RotX, RotY, RotZ);
    }

    public void cancelButtonClicked(View view){
        returnToMain();
    }

    public void returnToMain(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}