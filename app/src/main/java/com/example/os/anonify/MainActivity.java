package com.example.os.anonify;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements
        AnonChatFragment.OnFragmentInteractionListener,
        UserChatFragment.OnFragmentInteractionListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final String TAG = "";
    private Toolbar mainToolBar;
    private final int RESULT_LOAD_IMG = 1;
    private Uri selectedImage;
    public static Integer distance = 5;
    public static String latitude = "0", longitude = "0";
    public static String username = "";
    public static String profilePicUrl = "";

    //firebase stuff
    private FirebaseAuth mAuth;
    public static DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    public static FirebaseUser currentUser;

    //location
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;

    //fragments
    UserChatFragment userChatFragment;
    AnonChatFragment anonChatFragment;
    FragmentManager manager;
    static Boolean userFragmentSelected = true;

    //Tab items
    TabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //firebase auth
        mAuth = FirebaseAuth.getInstance();

        //firebase database connection
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //firebase storage
        mStorageRef = FirebaseStorage.getInstance().getReference();

        //add menu tool bar
        mainToolBar = findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mainToolBar);

        //location stuff
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //fragments
        userChatFragment = new UserChatFragment();
        anonChatFragment = new AnonChatFragment();
        manager = getSupportFragmentManager();

        //set tab position when main activity is opened
        //last fragment should be selected automatically when user comes back from chat activity
        FragmentTransaction transaction = manager.beginTransaction();
        tabs = findViewById(R.id.mainChatTabs);
        if(userFragmentSelected){
            TabLayout.Tab tab = tabs.getTabAt(0);
            tab.select();
            transaction.replace(R.id.mainFragmentLinearLayout, userChatFragment);
        }
        else{
            TabLayout.Tab tab = tabs.getTabAt(1);
            tab.select();
            transaction.replace(R.id.mainFragmentLinearLayout, anonChatFragment);
        }
        transaction.commit();

        //tablayout
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == 0){
                    userFragmentSelected = true;
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.mainFragmentLinearLayout, userChatFragment);
                    transaction.commit();
                }
                else{
                    userFragmentSelected = false;
                    FragmentTransaction transaction = manager.beginTransaction();
                    transaction.replace(R.id.mainFragmentLinearLayout, anonChatFragment);
                    transaction.commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        //connect GoogleApiClient
        mGoogleApiClient.connect();

        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = mAuth.getCurrentUser();

        //if user isn't authenticated
        if (currentUser == null) {
            sendStartActivity();
        } else {
            //checks location permissions
            if (checkLocationPermissions()) {
                //get current location
                getLastLocationNewMethod();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            //get last distance adjusted by user from database
            getAndSetLastDistance(mDatabase);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.menuItemLogout) {
            FirebaseAuth.getInstance().signOut();
            sendStartActivity();
        } else if (item.getItemId() == R.id.menuItemCreateAnonRoom) {
            createAnonChatRoom();
        } else if (item.getItemId() == R.id.menuItemAdjustDistance) {
            adjustDistance();
        } else if (item.getItemId() == R.id.menuItemChangePicture) {
            changeProfilePicture();
        }
        return true;
    }

    //It sends start activity when user wants to logout or isn't logged in.
    private void sendStartActivity() {
        //new intent is created from MainActivity to StartActivity
        Intent startIntent = new Intent(MainActivity.this, StartActivity.class);
        startActivity(startIntent);//Starts this intent
        finish(); //finishes MainActivity to block returning.
    }

    //create
    private void createAnonChatRoom(){
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_create_anon_chat_room, null);

        final EditText titleOfRoom = mView.findViewById(R.id.titleOfChatRoom);
        final Button createRoom = mView.findViewById(R.id.createChatRoom);

        mBuilder.setView(mView);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();
        createRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(titleOfRoom.getText().length() > 2){
                    String time = new SimpleDateFormat("yyyy-MM-dd-H-mm-ss-SSS").format(Calendar.getInstance().getTime());
                    Anon anonChatRoom = new Anon(latitude, longitude, titleOfRoom.getText().toString(), time);
                    mDatabase.child("anonchats").child(time).setValue(anonChatRoom);
                    dialog.dismiss();
                }
                else{
                    Toast.makeText(MainActivity.this, R.string.titleOfChatRoom, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // -------------- SET DISTANCE onStart() --------------

    private void getAndSetLastDistance(DatabaseReference mDatabase) {
        mDatabase.child("users").child(currentUser.getUid()).child("distance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                //set distance to value that user adjusted last time
                distance = Integer.parseInt(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // -------------- ADJUST DISTANCE --------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu); //connect main_menu layout
        return true;
    }

    private void adjustDistance() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_adjust_search_distance, null);

        final TextView searchDistanceText = mView.findViewById(R.id.searchDistanceTextView);
        searchDistanceText.setText(getResources().getString(R.string.searchDistance) + " " + distance.toString() + " km");

        final SeekBar sb = mView.findViewById(R.id.adjustDistanceSeekBar);
        sb.setProgress(distance); //sets initial value of SeekBar

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                distance = progress;
                mDatabase.child("users").child(currentUser.getUid()).child("distance").setValue(Integer.toString(distance));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                searchDistanceText.setText(getResources().getString(R.string.searchDistance) + " " + distance.toString() + " km");
            }
        });

        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }

    // --------------------------------------------------------

    // -------------- CHANGE PROFILE PICTURE --------------

    private void changeProfilePicture() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG); //it calls onActivityResult when image is selected
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                selectedImage = data.getData();
                uploadImage(selectedImage);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, R.string.wentWrong, Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(MainActivity.this, R.string.imageNotPicked, Toast.LENGTH_LONG).show();
        }
    }

    private void uploadImage(final Uri selectedImage) {
        //upload photo
        if (selectedImage != null) {
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
                            Toast.makeText(MainActivity.this, R.string.wentWrong, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    // ----------------------ALL LOCATION STUFF----------------------------------
    // For more information, check this answer --> https://stackoverflow.com/a/36110892/5219165

    private boolean checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            Toast.makeText(MainActivity.this, R.string.noLocation, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setLocation(String latitude, String longitude) {
        this.latitude = latitude.toString();
        this.longitude = longitude.toString();
    }

    private void getLastLocationNewMethod() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {
                            latitude = Double.toString(location.getLatitude());
                            longitude = Double.toString(location.getLongitude());
                            if(latitude.equals("0.0") || longitude.equals("0.0")){
                                Toast.makeText(MainActivity.this, R.string.noLocation, Toast.LENGTH_LONG).show();
                            }
                            //update current location of user in the database
                            mDatabase.child("users").child(currentUser.getUid()).child("latitude").setValue(latitude);
                            mDatabase.child("users").child(currentUser.getUid()).child("longitude").setValue(longitude);
                            setLocation(latitude, longitude);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        } startLocationUpdates();
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {
            latitude = Double.toString(mLocation.getLatitude());
            longitude = Double.toString(mLocation.getLongitude());
        } else {
            Toast.makeText(this, R.string.noLocation, Toast.LENGTH_LONG).show();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2)
                .setFastestInterval(2);

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onLocationChanged(Location location) {

    }

}
