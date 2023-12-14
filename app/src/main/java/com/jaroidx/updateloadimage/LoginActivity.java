package com.jaroidx.updateloadimage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jaroidx.updateloadimage.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding loginBinding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_login, null, false);
        loginBinding = DataBindingUtil.bind(view);
        setContentView(loginBinding.getRoot());
        setupFirebase();
        initView();
    }

    private void setupFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void initView() {
        fakeData();
        loginBinding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        loginBinding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    private void fakeData() {
        loginBinding.edtEmail.setText("hai@gmail.com");
        loginBinding.edtPassword.setText("123456");
    }

    private void register() {
        String userName = loginBinding.edtEmail.getText().toString();
        String passCode = loginBinding.edtPassword.getText().toString();
        firebaseAuth.createUserWithEmailAndPassword(userName, passCode).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "onComplete: " + task.getResult().getUser().getEmail());
                goToMainActivity();
            }
        }).addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e.getMessage()));
    }

    private void login() {
        String userName = loginBinding.edtEmail.getText().toString();
        String passCode = loginBinding.edtPassword.getText().toString();
        firebaseAuth.signInWithEmailAndPassword(userName, passCode).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "onComplete: " + task.getResult().getUser().getEmail());
                goToMainActivity();
            }
        }).addOnFailureListener(e -> Log.d(TAG, "onFailure: " + e.getMessage()));
    }


}