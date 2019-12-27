package com.myappcompany.hardi.recordaudio;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.myappcompany.hardi.recordaudio.model.User;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView username;

    FirebaseUser firebaseUser;
    DatabaseReference reference;

    //Record audio variables

    Button btnStartRecord, btnStopRecord, btnPlay;
    String pathSave="";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;


    private static final Float pitch=0.7f;
    
    final int REQUEST_PERMISSION_CODE=1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        username=findViewById(R.id.username);

        firebaseUser=FirebaseAuth.getInstance().getCurrentUser();
        reference= FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user=dataSnapshot.getValue(User.class);
                assert user != null;
                username.setText(user.getUsername());

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //Record Area

        if(!checkPermissionFromDevice()){
            requestPermission();
        }

        btnStartRecord=findViewById(R.id.btnStartRecord);
        btnStopRecord=findViewById(R.id.btnStopRecord);
        btnPlay=findViewById(R.id.btnPlay);




        btnStopRecord.setEnabled(false);
        btnPlay.setEnabled(false);

        btnStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkPermissionFromDevice()){


                    pathSave= Environment.getExternalStorageDirectory()
                            .getAbsolutePath()+"/"
                            + UUID.randomUUID().toString()+"_audio_record.wav";


                    setupMediaRecorder();



                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    btnStopRecord.setEnabled(true);
                    btnStartRecord.setEnabled(false);
                    btnPlay.setEnabled(false);

                    Toast.makeText(MainActivity.this, "Recording...",Toast.LENGTH_SHORT).show();

                }else {
                    requestPermission();
                }
            }
        });

        btnStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaRecorder.stop();
                btnStopRecord.setEnabled(false);
                btnPlay.setEnabled(true);
                btnStartRecord.setEnabled(true);
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnStopRecord.setEnabled(false);

                playAudio(pathSave);

                btnStartRecord.setEnabled(true);
                Toast.makeText(MainActivity.this, "Playing...", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private PlaybackParams playbackParams(MediaPlayer mediaPlayer){
        PlaybackParams params= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            params = new PlaybackParams();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            params.setPitch(pitch);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.setPlaybackParams(params);
        }
        return params;
    }



    private MediaPlayer playAudio(String pathSave){

            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(pathSave));
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            playbackParams(mediaPlayer);

            mediaPlayer.start();
        return mediaPlayer;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this,StartActivity.class));
                finish();
                return  true;
        }
        return false;
    }


    private boolean checkPermissionFromDevice() {
        int write_external_storage_result= ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result= ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return write_external_storage_result == PackageManager.PERMISSION_GRANTED && record_audio_result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO

        },REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_CODE:
                {
                    if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                        Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show();
                    else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }


    private void setupMediaRecorder() {
        mediaRecorder= new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(pathSave);
    }

}
