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

import java.util.ArrayList;
import java.util.Collections;

public class AnonChatFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    //listview
    private ListView listView;
    private MyListAdapter myAdapter;
    public ArrayList<ListViewElement> anonArrayList;

    //firebase stuff
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private FirebaseUser currentUser;

    //db
    private DatabaseHandler db;

    public AnonChatFragment() {
        // Required empty public constructor
    }

    public static AnonChatFragment newInstance(String param1, String param2) {
        AnonChatFragment fragment = new AnonChatFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_anon_chat, container, false);
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
        if (currentUser != null){
            //create user arraylist for listview
            anonArrayList = new ArrayList<>();

            //dummy user
            anonArrayList.add(new User("", "", "Loading... Please wait", "", "", "", ""));

            //create listview
            View v = getView();
            listView = getView().findViewById(R.id.anonChatListView);
            myAdapter = new MyListAdapter(getContext(), R.layout.chat_row, anonArrayList);
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
        if(!isNetworkAvailable(AnonChatFragment.this.getContext())){
            createListViewAgain(db.getAnonArrayList());
            Toast.makeText(getContext(), R.string.noInternetConnection, Toast.LENGTH_SHORT).show();
        }

        mDatabase.child("anonchats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //create a new userlist
                anonArrayList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String latitudeR = snapshot.child("latitude").getValue().toString();
                    String longitudeR = snapshot.child("longitude").getValue().toString();
                    String titleR = snapshot.child("title").getValue().toString();
                    String chatID = snapshot.getKey().toString();

                    //create a new user with received data
                    Anon anonChatRoom = new Anon(latitudeR, longitudeR, titleR, chatID);

                    //calculate distance between user of app and other user in the database
                    Double distanceBetween = Calculators.returnDistance(
                            Double.parseDouble(MainActivity.latitude), Double.parseDouble(MainActivity.longitude),
                            Double.parseDouble(latitudeR), Double.parseDouble(longitudeR), 'K');

                    if (distanceBetween <= MainActivity.distance) {
                        anonArrayList.add(anonChatRoom);
                        if(!db.anonChatExits(chatID)){
                            db.insertAnonChat(anonChatRoom);
                        }
                    }
                }
                Collections.reverse(anonArrayList);
                createListViewAgain(anonArrayList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Update userlist

    private void createListViewAgain(ArrayList<ListViewElement> anonArrayList) {
        //create listview
        if(getView() != null) {
            listView = getView().findViewById(R.id.anonChatListView);
            if (listView != null) {
                myAdapter = new MyListAdapter(getContext(), R.layout.chat_row, anonArrayList);
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
