package com.jaroidx.updateloadimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jaroidx.updateloadimage.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private Uri mUri = null;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private final ActivityResultLauncher<String> pickImageResultLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (result != null) {
            mUri = result;
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Glide.with(this).load(bitmap).centerCrop().into(binding.imgAvatar);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_main, null, false);
        binding = DataBindingUtil.bind(view);
        setContentView(binding.getRoot());
        setupFirebase();
        initView();
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseStorage = FirebaseStorage.getInstance();
    }

    private void initView() {
        bindUserData();

        binding.btnLogout.setOnClickListener(v -> logout());

        binding.imgAvatar.setOnClickListener(v -> openGallery());

        binding.btnSave.setOnClickListener(v -> save());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bindUserData() {
        showLoading();
        Uri uri = currentUser.getPhotoUrl();
        Glide.with(this)
                .load(uri != null ? uri : R.drawable.ic_launcher_background)
                .placeholder(R.drawable.ic_launcher_background)
                .addListener(requestListener)
                .into(binding.imgAvatar);
    }

    private final RequestListener requestListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            hideLoading();
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            hideLoading();
            return false;
        }
    };

    private void save() {
        if (mUri != null) {
            showLoading();
            updateLoadToFireStore();
        }
    }

    private void showLoading() {
        binding.llLoading.setVisibility(View.VISIBLE);
        changeStateButton(false);
    }

    private void hideLoading() {
        binding.llLoading.setVisibility(View.GONE);
        changeStateButton(true);
    }

    private void changeStateButton(boolean enable) {
        binding.btnLogout.setEnabled(enable);
        binding.btnSave.setEnabled(enable);
        binding.imgAvatar.setEnabled(enable);
    }

    private void updateLoadToFireStore() {
        // Create or update a reference to the image in Firebase Storage
        StorageReference storageReference = firebaseStorage.getReference("profile_pictures" + currentUser.getUid() + ".jpg");

        // Upload the image
        UploadTask uploadTask = storageReference.putFile(mUri);
        uploadTask.continueWithTask(task -> {
            if (task.isSuccessful()) {
                //Get download url from firebase storage
                Log.d(TAG, "Url: " + task.getResult().getStorage().getDownloadUrl());
                return task.getResult().getStorage().getDownloadUrl();
            }
            throw task.getException();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                //Update user profile
                updateUserProfile(downloadUri);
            }
            hideLoading();
        }).addOnFailureListener(e -> {
            Log.d(TAG, "onFailure: " + e.getMessage());
            hideLoading();
        });
    }

    private void updateUserProfile(Uri uri) {
        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setPhotoUri(uri).build();

        currentUser.updateProfile(profileChangeRequest).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "updateUserProfile: update profile success");
            }
            hideLoading();
        }).addOnFailureListener(e -> {
            Log.d(TAG, "onFailure: " + e.getMessage());
            hideLoading();
        });
    }

    private void openGallery() {
        pickImageResultLauncher.launch("image/*");
    }
}