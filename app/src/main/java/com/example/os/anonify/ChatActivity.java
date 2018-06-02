package com.example.os.anonify;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private Toolbar chatToolBar;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private ArrayList<Message> messageList;
    private LinearLayoutManager layoutManager;

    private EditText messageTextBox;
    private Button messageSendButton;
    private LinearLayout chatBoxLinearLayout;

    String title, emailOfCurrentUser, emailOfOtherUser, chatID, senderProfilePic;

    Boolean userChat=false; //If it is normal chat, it will be true

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        emailOfCurrentUser = intent.getStringExtra("emailOfCurrentUser");
        emailOfOtherUser = intent.getStringExtra("emailOfOtherUser"); //If it is anon chat room, emailOfOtherUser will be null
        chatID = intent.getStringExtra("chatID"); //If it is normal chat, chatID will be null
        senderProfilePic = intent.getStringExtra("senderProfilePic");

        //set message textBox and send button
        messageTextBox = findViewById(R.id.edittext_chatbox);
        messageSendButton = findViewById(R.id.button_chatbox_send);
        chatBoxLinearLayout = findViewById(R.id.layout_chatbox);

        //set whether it is user chat or anon chat
        userChat = emailOfOtherUser != null ? true : false;

        //creates an empty message list
        messageList = new ArrayList<>();

        setButtonOnClickListener();

        createToolBar();

        //creates user chat arraylist or anon chat arraylist
        if(userChat) createUserMessageList();
        else createAnonMessageList();

        createListView();


    }

    private void setButtonOnClickListener(){
        messageSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If it is userchat
                if(userChat){
                    createUserMessage();
                }
                //If it is anonymous chat room
                else{
                    createAnonVoid();
                }
            }
        });
    }

    private void createToolBar(){
        //sets toolbar
        chatToolBar = findViewById(R.id.chat_page_toolbar);
        setSupportActionBar(chatToolBar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void createUserMessage(){
        String message = messageTextBox.getText().toString();

        if(message.length() > 0){

            message = message.trim(); // delete all spaces at the beginning and end of string

            final String e1 = SignupActivity.returnValidPath(emailOfCurrentUser);
            final String e2 = SignupActivity.returnValidPath(emailOfOtherUser);
            messageTextBox.setText("");
            String currentTime = Calculators.getTime();
            final SimpleMessage m = new SimpleMessage(message, currentTime, MainActivity.username);

            //Don't use addValueEventListener(). It causes infinite loop
            //addListenerForSingleValueEvent calls onDataChange once and doesn't call it again when data of instance is changed
            //It stops listening to the reference location it is attached to.
            //So, it doesn't cause infinite loop
            //See for more information --> https://stackoverflow.com/questions/41579000/difference-between-addvalueeventlistener-and-addlistenerforsinglevalueevent
            MainActivity.mDatabase.child("userchats").child(e1+e2).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //There are two possibilities of chat key between these users
                    //Chat key between these users may be email of current user + email of other user
                    //Or it may be email of other user + email of current user
                    //It checks which one exists in the database
                    //And inserts messages there
                    if (dataSnapshot.exists()) {
                        DatabaseReference r = MainActivity.mDatabase
                                .child("userchats")
                                .child(e1+e2);
                        r.push().setValue(m);
                    }
                    else {
                        DatabaseReference r = MainActivity.mDatabase
                                .child("userchats")
                                .child(e2+e1);
                        r.push().setValue(m);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
        else{
            Toast.makeText(this, R.string.blankMessageError, Toast.LENGTH_SHORT).show();
        }
    }

    private void createAnonVoid(){
        String message = messageTextBox.getText().toString();
        messageTextBox.setText("");

        //returns gmt time
        String currentTime = Calculators.getTime();
        SimpleMessage m = new SimpleMessage(message, currentTime, MainActivity.username);
        DatabaseReference r = MainActivity.mDatabase
                .child("anonchats")
                .child(chatID)
                .child("messages");
        r.push().setValue(m);
    }

    private void createUserMessageList(){
        final String e1 = SignupActivity.returnValidPath(emailOfCurrentUser);
        final String e2 = SignupActivity.returnValidPath(emailOfOtherUser);

        MainActivity.mDatabase.child("userchats").child(e1+e2).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    createListFrom(e1+e2);
                }
                else {
                    createListFrom(e2+e1);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    private void createListFrom(String path){
        MainActivity.mDatabase.child("userchats").child(path)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //creates a message list
                        messageList = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            String message = snapshot.child("message").getValue().toString();
                            String time = snapshot.child("time").getValue().toString();
                            String username = snapshot.child("username").getValue().toString();
                            String emailOfUser = "";

                            //creates a hour and minute with given string
                            String hourAndMinute = Calculators.returnHourAndMinute(time);

                            //if it is current user's message
                            if(username.equals(MainActivity.username)) {
                                emailOfUser = MainActivity.currentUser.getEmail();
                            }

                            //create a new user with received data
                            User u = new User("", emailOfUser, username, "", "", "",senderProfilePic);
                            Message m = new Message(u, message, hourAndMinute, username, senderProfilePic);
                            messageList.add(m);
                        }

                        createListView();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void createAnonMessageList(){

        MainActivity.mDatabase.child("anonchats").child(chatID).child("messages")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //creates a message list
                messageList = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue().toString();
                    String time = snapshot.child("time").getValue().toString();

                    //creates a hour and minute with given string
                    String hourAndMinute = Calculators.returnHourAndMinute(time);

                    //create a new user with received data
                    User u = new User("", "", "", "", "", "","");
                    Message m = new Message(u, message, hourAndMinute, "", "");
                    messageList.add(m);
                }
                createListView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void createListView(){

        //creates the listview
        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        layoutManager = new LinearLayoutManager(this);
        mMessageRecycler.setLayoutManager(layoutManager);
        mMessageAdapter = new MessageListAdapter(this, messageList, userChat);
        mMessageRecycler.setHasFixedSize(true);
        mMessageRecycler.setAdapter(mMessageAdapter);
        scrollToBottom(layoutManager, mMessageRecycler, mMessageAdapter);

        //adjusts space when keyboard is opened
        if (Build.VERSION.SDK_INT >= 11) {
            mMessageRecycler.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom < oldBottom) scrollToBottom(layoutManager, mMessageRecycler, mMessageAdapter);
                }
            });
        }
    }

    private void scrollToBottom(LinearLayoutManager layoutManager, RecyclerView mRecyclerView, MessageListAdapter mAdapter) {
        layoutManager.smoothScrollToPosition(mRecyclerView, null, mAdapter.getItemCount());

    }
}