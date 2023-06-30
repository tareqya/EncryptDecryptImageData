package com.tareq.encryptdecryptimages;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class EncryptFragment extends Fragment {

    private MaterialButton encrypt_BTN_encrypt;
    private ImageView encrypt_img_attached;
    private EditText encrypt_TXT_text;

    private Bitmap selectedImage;
    private Activity activity;

    private ActivityResultLauncher<Intent> resultLauncher;


    public EncryptFragment(Activity activity) {
        // Required empty public constructor
        this.activity = activity;
        this.selectedImage = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_encrypt, container, false);
        findViews(root);
        initVars();

        return root;
    }

    private void saveImageToGallery(Bitmap bitmap) {
        // Get the current timestamp to use as the image file name
        String fileName = "IMG_" + new Date().getTime() + ".jpg";

        // Get the directory path where the image will be saved
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();

        // Create the file object within the specified directory
        File imageFile = new File(directory, fileName);

        // Save the bitmap to the file
        try (OutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();

            // Notify the media scanner about the new image file
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION, "Image saved from app");
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            ContentResolver contentResolver = activity.getContentResolver();
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            // Scan the newly saved image file to appear in the gallery
            MediaScannerConnection.scanFile(activity, new String[]{imageFile.getAbsolutePath()}, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initVars() {
        registerResult();

        encrypt_BTN_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String secretText = encrypt_TXT_text.getText().toString();
                if(!secretText.equals("") && selectedImage != null){
                    Bitmap encrypted_image = ImageEncryption.encryptText(selectedImage, secretText);
                    saveImageToGallery(encrypted_image);
                    Toast.makeText(activity, "Encrypted image saved!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        encrypt_img_attached.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED){
                    Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                    resultLauncher.launch(intent);
                }else {
                    Toast.makeText(activity, "No permissions to access gallery!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void findViews(View root) {
        encrypt_BTN_encrypt = root.findViewById(R.id.encrypt_BTN_encrypt);
        encrypt_img_attached = root.findViewById(R.id.encrypt_img_attached);
        encrypt_TXT_text = root.findViewById(R.id.encrypt_TXT_text);

    }

    public void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try{
                            Uri imageUri = result.getData().getData();
                            InputStream inputStream =activity.getContentResolver().openInputStream(imageUri);
                            selectedImage = BitmapFactory.decodeStream(inputStream);
                            inputStream.close();
                            encrypt_img_attached.setImageBitmap(selectedImage);

                        }catch (Exception e){
                            Toast.makeText(activity, "No image selected!", Toast.LENGTH_LONG).show();
                            encrypt_img_attached.setImageResource(R.drawable.attach_image);
                        }
                    }
                }
        );
    }

    public void clean(){
        encrypt_img_attached.setImageResource(R.drawable.attach_image);
        encrypt_TXT_text.setText("");
    }
}