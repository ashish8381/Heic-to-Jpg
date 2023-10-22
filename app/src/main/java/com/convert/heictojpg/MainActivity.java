package com.convert.heictojpg;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST = 1;
    TextView mprogresstext;
    private List<Uri> selectedHEICFiles = new ArrayList<>();
    int totalimg=0;

    String filename="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mprogresstext=findViewById(R.id.progressimagename);
        Button convertButton = findViewById(R.id.convertButton);
        convertButton.setOnClickListener(v -> {
            // Launch the file picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/heic"); // Restrict to HEIC format
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, FILE_PICKER_REQUEST);
        });


        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    for (Uri imageUri : imageUris) {
                        // Handle each shared image (imageUri) and convert it to JPG
                        selectedHEICFiles.add(imageUri);
                    }
                }
            }
        } else if (Intent.ACTION_SEND.equals(action) && type != null && "image/heic".equals(type)) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                // Handle the shared image (imageUri) and convert it to JPG
                selectedHEICFiles.add(imageUri);
            }
        }


        if(selectedHEICFiles.size()>0){
            List<String> jpgPaths = convertHEICsToJPGs(selectedHEICFiles);
            mprogresstext.setText("Successfully Converted!!");
        }



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        selectedHEICFiles.add(uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    selectedHEICFiles.add(uri);
                }

                totalimg=selectedHEICFiles.size();

                // Convert the selected HEIC files to JPG
                List<String> jpgPaths = convertHEICsToJPGs(selectedHEICFiles);
                mprogresstext.setText("Successfully Converted!!");
                // Process the converted JPGs as needed
            }
        }
    }

    private List<String> convertHEICsToJPGs(List<Uri> heicUris) {
        mprogresstext.setVisibility(View.VISIBLE);
        List<String> jpgPaths = new ArrayList<>();
        int count=1;
        for (Uri heicUri : heicUris) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(heicUri);
                if (inputStream != null) {
                    File jpgFile = createJPGFile();
                    FileOutputStream outputStream = new FileOutputStream(jpgFile);

                    // Convert HEIC to JPG and save the bytes to the output stream
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    outputStream.close();
                    int finalCount = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mprogresstext.setVisibility(View.VISIBLE);
                            mprogresstext.setText("Saved: " + filename + " " + finalCount + "/" + totalimg);
                        }
                    });
                    Log.e("motu","Saved :"+jpgFile.getName()+" "+count+"/"+totalimg);
                    count++;
                    jpgPaths.add(jpgFile.getPath());

                    // Display a success message as a Toast

                    MediaScannerConnection.scanFile(
                            this,
                            new String[] { jpgFile.getAbsolutePath() },
                            null,
                            (path, uri) -> {

                               // showToast("Image saved: " + jpgFile.getName());
                            }
                    );



                   // showToast("Image saved: " + jpgFile.getName());

                    inputStream.close();
                } else {
                    // Handle the case where the input stream is null (file not found)
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jpgPaths;
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private File createJPGFile() {
        File downloadsDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Heic to Jpg");
        if (!downloadsDirectory.exists()) {
            downloadsDirectory.mkdirs();
        }
        return new File(downloadsDirectory, "Converted_"+System.currentTimeMillis()+".jpg");
    }

}