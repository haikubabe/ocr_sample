package com.sreemoyee.ocrsample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "Text API";
    private TextView scanResults;
    private ImageView imageView;
    private Uri imageUri;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private static final int PHOTO_REQUEST = 10;
    public static final int REQUEST_CODE_GALLERY = 0x1;
    private String userChosenTask = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button scanTextButton = findViewById(R.id.scanText);
        Button captureTextButton = findViewById(R.id.captureText);
        scanResults = findViewById(R.id.results);
        imageView = findViewById(R.id.image);
        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }

        scanTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userChosenTask = "Choose From Gallery";
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });
        captureTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userChosenTask = "Take Photo";
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChosenTask.equals("Choose From Gallery")) {
                        openGallery();
                    } else if (userChosenTask.equals("Take Photo")) {
                        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
                        if (rc == PackageManager.PERMISSION_GRANTED) {
                            takePicture();
                        } else {
                            requestCameraPermission();
                        }
                    }
                }
                break;
            case REQUEST_CAMERA_PERMISSION:
                takePicture();
                break;
        }
    }

    private void requestCameraPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
    }

    public void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
        imageUri = FileProvider.getUriForFile(MainActivity.this,
                BuildConfig.APPLICATION_ID + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, PHOTO_REQUEST);
    }

    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PHOTO_REQUEST:
                    launchMediaScanIntent();
                    processingTextFromImage(imageUri);
                    break;
                case REQUEST_CODE_GALLERY:
                    processingTextFromImage(data.getData());
                    break;
            }
        }
    }

    private void processingTextFromImage(Uri imageUri) {
        try {
            Bitmap bitmap = decodeBitmapUri(this, imageUri);
            imageView.setImageBitmap(bitmap);
            //Create the TextRecognizer
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            //check if TextRecognizer is operational or not
            if (textRecognizer.isOperational() && bitmap != null) {
                //read the text from the bitmap
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                //call the detect method of the TextRecognizer to generate a sparse array
                // containing all the text the TextRecognizer detected in the photo
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock textBlock = textBlocks.get(index);
                    if (textBlock == null)
                        continue;
                    blocks = blocks + textBlock.getValue() + "\n\n";
                    for (Text line : textBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text word : line.getComponents()) {
                            //extract scanned text words here
                            words = words + word.getValue() + ", ";
                        }
                    }
                }
                if (textBlocks.size() == 0) {
                    scanResults.setText("Scan Failed: Found nothing to scan");
                } else {
                    scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
                    scanResults.setText(scanResults.getText() + blocks + "\n");
                    scanResults.setText(scanResults.getText() + "---------" + "\n");
                        /*scanResults.setText(scanResults.getText() + "Lines: " + "\n");
                        scanResults.setText(scanResults.getText() + lines + "\n");
                        scanResults.setText(scanResults.getText() + "---------" + "\n");
                        scanResults.setText(scanResults.getText() + "Words: " + "\n");
                        scanResults.setText(scanResults.getText() + words + "\n");
                        scanResults.setText(scanResults.getText() + "---------" + "\n");*/
                }
            } else {
                scanResults.setText("Could not set up TextRecognizer!");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
            Log.e(LOG_TAG, e.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, imageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }
}
