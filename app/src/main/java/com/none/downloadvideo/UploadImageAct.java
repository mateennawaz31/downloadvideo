package com.none.downloadvideo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UploadImageAct extends AppCompatActivity {

    private Button uploadBtn;
    private EditText imageNameET;

    private ImageView imageToUploadIV;
    private Dialog objectWaitDialog;

    private static final int REQUEST_CODE=123;
    private Uri objectUri;

    private boolean isImageSelected=false;
    private FirebaseFirestore objectFirebaseFirestore;

    private StorageReference objectStorageReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        uploadBtn=findViewById(R.id.uploadImageBtn);
        imageNameET=findViewById(R.id.imageNameET);

        imageToUploadIV=findViewById(R.id.imagetoUploadIV);
        objectWaitDialog=new Dialog(this);

        objectWaitDialog.setContentView(R.layout.please_wait);
        objectWaitDialog.setCancelable(false);

        imageToUploadIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImageFromGallery();
            }
        });

        objectFirebaseFirestore=FirebaseFirestore.getInstance();
        objectStorageReference= FirebaseStorage.getInstance().getReference("MyImages");

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageToCloudStorage();
            }
        });
    }

    private void uploadImageToCloudStorage()
    {
        if(isImageSelected && !imageNameET.getText().toString().isEmpty())
        {
            objectWaitDialog.show();
            String imagePath=imageNameET.getText().toString()+"."+getExtension(objectUri);
            final StorageReference imageRef=objectStorageReference.child(imagePath);

            UploadTask objectUploadTask=imageRef.putFile(objectUri);
            objectUploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful())
                    {
                        objectWaitDialog.dismiss();
                        throw task.getException();
                    }

                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful())
                    {
                        Map<String,Object> objectMap=new HashMap<>();
                        objectMap.put("url",task.getResult().toString());

                        objectFirebaseFirestore.collection("Links")
                                .document(imageNameET.getText().toString())
                                .set(objectMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        objectWaitDialog.dismiss();
                                        Toast.makeText(UploadImageAct.this, "Image uploaded successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        objectWaitDialog.dismiss();
                                        Toast.makeText(UploadImageAct.this, "Fails to upload image:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    objectWaitDialog.dismiss();
                    Toast.makeText(UploadImageAct.this, "Fails to upload image:"+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
            ;

        }
        else if(!isImageSelected)
        {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
        else if(imageNameET.getText().toString().isEmpty())
        {
            Toast.makeText(this, "Please enter the image name", Toast.LENGTH_SHORT).show();
        }
    }

    private String getExtension(Uri objectUri) {
        try
        {
            ContentResolver objectContentResolver=getContentResolver();
            MimeTypeMap objectMimeTypeMap=MimeTypeMap.getSingleton();

            String extension=objectMimeTypeMap.getExtensionFromMimeType(objectContentResolver.getType(objectUri));
            return extension;
        }
        catch (Exception e)
        {
            Toast.makeText(this, "getExtension:"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    private void selectImageFromGallery()
    {
        try
        {
            Intent objectIntent=new Intent();
            objectIntent.setType("video/*");

            objectIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(objectIntent,REQUEST_CODE);
        }
        catch (Exception e)
        {
            Toast.makeText(this, "selectImageFromGallery:"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE && resultCode==RESULT_OK && data!=null)
        {
            objectUri=data.getData();
            if(objectUri!=null)
            {
                try
                {
                    Bitmap objectBitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),objectUri);
                    imageToUploadIV.setImageBitmap(objectBitmap);

                    isImageSelected=true;
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                Toast.makeText(this, "Data is null", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(this, "No Image is selected", Toast.LENGTH_SHORT).show();
        }
    }
}
