package com.example.os.anonify;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;

public class UserChatFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    //listview
    private ListView listView;
    private MyListAdapter myAdapter;
    public ArrayList<ListViewElement> userArrayList;

    //firebase stuff
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private FirebaseUser currentUser;
    private DatabaseHandler db;

    public UserChatFragment() {
        // Required empty public constructor
    }

    public static UserChatFragment newInstance(String param1, String param2) {
        UserChatFragment fragment = new UserChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        db = DatabaseHandler.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_chat, container, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onStart(){
        super.onStart();
        //firebase auth

        mAuth = FirebaseAuth.getInstance();

        //firebase database connection
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //firebase storage
        mStorageRef = FirebaseStorage.getInstance().getReference();

        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = mAuth.getCurrentUser();

        //if user isn't authenticated
        if (currentUser != null) {
            //create user arraylist for listview
            userArrayList = new ArrayList<>();

            //dummy user
            userArrayList.add(new User("", "", "Loading... Please wait", "", "", "", ""));

            //create listview
            View v = getView();
            listView = getView().findViewById(R.id.userChatListView);
            myAdapter = new MyListAdapter(getContext(), R.layout.chat_row, userArrayList);
            listView.setAdapter(myAdapter);

            //get last distance adjusted by user from database
            getAndSetLastDistance(mDatabase);

            //create or update userList
            updateListView();
        }
    }

    private void getAndSetLastDistance(DatabaseReference mDatabase) {
        mDatabase.child("users").child(currentUser.getUid()).child("distance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                //set distance to value that user adjusted last time
                MainActivity.distance = Integer.parseInt(dataSnapshot.getValue(String.class));
                updateListView();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateListView() {

        //If internet isn't available, get chats from database
        if(!isNetworkAvailable(UserChatFragment.this.getContext())){
            createListViewAgain(db.getUserArrayList());
            Toast.makeText(getContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
        }

        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //get email from local storage
                String email = "";
                try {
                    FileInputStream fin = getContext().openFileInput("email");
                    int c;
                    while ((c = fin.read()) != -1) {
                        email = email + Character.toString((char) c);
                    }
                } catch (Exception e) {
                }

                //create a new userlist
                userArrayList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String distanceU = snapshot.child("distance").getValue().toString();
                    String emailU = snapshot.child("email").getValue().toString();
                    String latitudeU = snapshot.child("latitude").getValue().toString();
                    String longitudeU = snapshot.child("longitude").getValue().toString();
                    String profilePicUrlU = snapshot.child("profilePicUrl").getValue().toString();
                    String usernameU = snapshot.child("username").getValue().toString();
                    String uidU = snapshot.getKey().toString();

                    //create a new user with received data
                    User user = new User(uidU, emailU, usernameU, distanceU, latitudeU, longitudeU, profilePicUrlU);

                    //calculate distance between user of app and other user in the database
                    Double distanceBetween = Calculators.returnDistance(
                            Double.parseDouble(MainActivity.latitude), Double.parseDouble(MainActivity.longitude),
                            Double.parseDouble(latitudeU), Double.parseDouble(longitudeU), 'K');

                    if (distanceBetween <= MainActivity.distance && !email.equals(emailU)) {
                        userArrayList.add(user);
                        if(!db.userExits(uidU)){
                            db.insertUser(user);
                        }
                    }
                    //It means that this is current user
                    else if(email.equals(emailU)){
                        //Set current user's username
                        MainActivity.username = usernameU;
                        MainActivity.profilePicUrl = profilePicUrlU;
                    }
                }
                Collections.reverse(userArrayList);
                createListViewAgain(userArrayList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //Update userlist

    private void createListViewAgain(ArrayList<ListViewElement> userArrayList) {
        //create listview
        if(getView() != null) {
            listView = getView().findViewById(R.id.userChatListView);
            if(listView != null){
                myAdapter = new MyListAdapter(getContext(), R.layout.chat_row, userArrayList);
                listView.setAdapter(myAdapter);
            }
        }
    }

    public boolean isNetworkAvailable(Context context) {
        if(context != null){
            ConnectivityManager connectivity =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivity == null) {
                return false;
            } else {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }
}
