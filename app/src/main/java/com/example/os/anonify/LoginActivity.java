package com.example.os.anonify;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.FileOutputStream;

public class LoginActivity extends AppCompatActivity {

    private EditText emailET, passwordET;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private Toolbar loginToolBar;
    private ProgressDialog progressD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        progressD = new ProgressDialog(this);
        loginToolBar = findViewById(R.id.login_page_toolbar);
        setSupportActionBar(loginToolBar);
        getSupportActionBar().setTitle("Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        emailET = findViewById(R.id.loginEmail);
        passwordET = findViewById(R.id.loginPassword);
        loginButton = findViewById(R.id.loginLoginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password;
                email = emailET.getText().toString();
                password = passwordET.getText().toString();

                loginUser(email, password);
            }
        });

    }

    private void loginUser(final String email, final String password){

        if(email.length() != 0 && password.length() != 0){

            progressD.setTitle("Let's Login!");
            progressD.setMessage("Please wait...");
            progressD.setCanceledOnTouchOutside(false);
            progressD.show();

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this,new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    //If user logins successfully
                    if(task.isSuccessful()){

                        //save email to local storage
                        try {
                            FileOutputStream fOut = openFileOutput("email", Context.MODE_PRIVATE);
                            fOut.write(email.getBytes());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }

                        progressD.dismiss();
                        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // to prevent go back after logged in
                        startActivity(mainIntent);
                        finish();
                    }
                    else{
                        progressD.hide();
                        Exception e = task.getException();
                        String errorMessage = e.getMessage();
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else{
            Toast.makeText(LoginActivity.this, R.string.pleaseFillAllFields, Toast.LENGTH_SHORT).show();
        }
    }
}
