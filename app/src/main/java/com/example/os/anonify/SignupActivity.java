package com.example.os.anonify;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameET, emailET, passwordET;
    private Button signupButton;
    private CircleImageView profileImage;
    private Toolbar signupToolBar;
    private ProgressDialog progressD;
    private Uri selectedImage = null;
    private final int RESULT_LOAD_IMG = 1;
    private String latitude="0", longitude="0";

    //firebase stuff
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        //firebase auth
        mAuth = FirebaseAuth.getInstance();

        //firebase database connection
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //firebase storage
        mStorageRef = FirebaseStorage.getInstance().getReference();

        //progress dialog
        progressD = new ProgressDialog(this);

        //Signup toolbar
        signupToolBar = findViewById(R.id.signup_page_toolbar);
        setSupportActionBar(signupToolBar);
        getSupportActionBar().setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Edit texts and buttons
        usernameET = findViewById(R.id.signupUsername);
        emailET = findViewById(R.id.signupEmail);
        passwordET = findViewById(R.id.signupPassword);
        signupButton = findViewById(R.id.signupSignupButton);
        profileImage = findViewById(R.id.profile_image);

        //Listeners
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeProfilePicture();
            }
        });

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubmit();
            }
        });
    }

    // -------------- CHANGE PROFILE PICTURE --------------

    private void changeProfilePicture(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG); //it calls onActivityResult when image is selected
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                selectedImage = data.getData(); // sets selectedImage
                profileImage.setImageURI(selectedImage); //changes image with selected one
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(SignupActivity.this, R.string.wentWrong, Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(SignupActivity.this, R.string.imageNotPicked,Toast.LENGTH_LONG).show();
        }
    }

    // --------------------------------------------------------

    private boolean checkLocationPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return false;
        }
        return true;
    }

    private void handleSubmit(){

        // first check for location permissions
        if(!checkLocationPermissions()){
            Toast.makeText(SignupActivity.this, R.string.imageNotPicked, Toast.LENGTH_SHORT).show();
            return;
        }

        //if profile pic isn't selected
        if(selectedImage == null){
            Toast.makeText(SignupActivity.this, R.string.imageNotPicked, Toast.LENGTH_SHORT).show();
            return;
        }

        final String username, email, password;
        username = usernameET.getText().toString();
        email = emailET.getText().toString();
        password = passwordET.getText().toString();

        if(username.length() != 0 && email.length() != 0 && password.length() != 0){

            String validUsernamePathForFirebase = returnValidPath(username);
            final boolean[] signedUp = {false};
            mDatabase.child("usernames").child(validUsernamePathForFirebase).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //If username already exists, it shows a message
                    if (dataSnapshot.exists()) {
                        if(!signedUp[0])
                        Toast.makeText(SignupActivity.this, R.string.usernameExits, Toast.LENGTH_SHORT).show();
                    }
                    else {
                        signedUp[0] = true;
                        signup(username, email, password);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
        else{
            Toast.makeText(SignupActivity.this, R.string.pleaseFillAllFields, Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage(final Uri selectedImage){
        //upload photo
        if(selectedImage != null){
            StorageReference profilePicRef = mStorageRef.child("profilePics/" + currentUser.getUid() + ".jpg");
            profilePicRef.putFile(selectedImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Get a URL to the uploaded content
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    //update user's profile picture url in the database
                    mDatabase.child("users").child(currentUser.getUid()).child("profilePicUrl").setValue(downloadUrl.toString());
                }
            }).
                    addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            Toast.makeText(SignupActivity.this, R.string.wentWrong, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void signup(final String username, final String email, final String password){
        progressD.setTitle("Let's Sign Up!");
        progressD.setMessage("Please wait...");
        progressD.setCanceledOnTouchOutside(false);
        progressD.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                //If user registered successfully
                if(task.isSuccessful()){
                    progressD.dismiss();

                    //set uid of registered user
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();

                    //create a new user object
                    User u = new User(uid ,email, username, "5", latitude, longitude, "");

                    //create a new user object in the database with uid
                    mDatabase.child("users").child(uid).setValue(u);

                    //add username to usernames
                    String validUsernamePathForFirebase = returnValidPath(username); //Firebase Database paths must not contain . # $ [ or ]
                    mDatabase.child("usernames").child(validUsernamePathForFirebase).setValue("true");

                    //set current user
                    currentUser = task.getResult().getUser();
                    //upload image to storage
                    uploadImage(selectedImage);

                    //save username to local storage
                    try {
                        FileOutputStream fOut = openFileOutput("email", Context.MODE_PRIVATE);
                        fOut.write(email.getBytes());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    //navigate to MainActivity
                    Intent mainIntent = new Intent(SignupActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // to prevent go back after signed up
                    startActivity(mainIntent);
                    finish();
                }
                else{
                    progressD.hide();
                    Exception e = task.getException();
                    String errorMessage = e.getMessage();
                    Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public static String returnValidPath(String s){
        String validS = "";
        for(int i=0; i<s.length(); i++){
            char current = s.charAt(i);
            if(current == '.' || current == '#' || current == '$' || current == '[' || current == ']'){
                validS += "_";
            }
            else{
                validS += s.charAt(i);
            }
        }
        return validS;
    }

}
