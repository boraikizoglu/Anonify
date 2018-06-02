package com.example.os.anonify;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

class MyListAdapter extends ArrayAdapter<ListViewElement> {


    private int layout;
    private String listType; //It will be userChats or anonChats
    public MyListAdapter(@NonNull Context context, int resource, @NonNull List<ListViewElement> objects) {
        super(context, resource, objects);
        layout = resource;
        this.listType = listType;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder mainViewholder = null;

        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.rowItself = convertView.findViewById(R.id.mainRowLinearLayout);
            viewHolder.rowImage = convertView.findViewById(R.id.mainRowImage);
            viewHolder.rowTitle = convertView.findViewById(R.id.mainRowTitle);
            convertView.setTag(viewHolder);
        }

        mainViewholder = (ViewHolder) convertView.getTag();

        //sets row image and title
        final String imageUrl = getItem(position).getProfilePicUrl();
        mainViewholder.rowTitle.setText(getItem(position).getTitle());

        //If it is userChats listview, imageUrl of listview object(user) won't be ""
        //ImageUrl will be "" for anon objects.
        //If it is anonymous chat room, it will show default anonymous avatar
        if(imageUrl.length() != 0){
            Picasso.get().load(imageUrl).into(mainViewholder.rowImage);
        }

        mainViewholder.rowItself.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chatKey = getItem(position).getChatKey();
                String title = getItem(position).getTitle();
                String currentUserEmail = MainActivity.currentUser.getEmail();
                Intent chatPageIntent = new Intent(getContext(), ChatActivity.class);
                chatPageIntent.putExtra("emailOfCurrentUser", currentUserEmail);
                chatPageIntent.putExtra("title", title);
                chatPageIntent.putExtra("senderProfilePic", getItem(position).getProfilePicUrl());

                //if it is anon chat room
                if(getItem(position) instanceof Anon){
                    //redirect user to generic chat page with chatKey(chatid of anon room) and email of current user
                    chatPageIntent.putExtra("chatID", chatKey);
                    getContext().startActivity(chatPageIntent);
                }
                else{
                    //redirect user to generic chat page with email of current user(email) and email of other user
                    //we will use these two variables to get messages between users from database
                    chatPageIntent.putExtra("emailOfOtherUser", chatKey);
                    getContext().startActivity(chatPageIntent);
                }
            }
        });

        return convertView;
    }

}

class ViewHolder {
    LinearLayout rowItself;
    CircleImageView rowImage;
    TextView rowTitle;
}