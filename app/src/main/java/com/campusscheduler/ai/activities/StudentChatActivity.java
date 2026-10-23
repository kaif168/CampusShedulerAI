package com.campusscheduler.ai.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.campusscheduler.ai.R;
import com.campusscheduler.ai.adapters.GenericAdapter;
import com.campusscheduler.ai.engine.AcademicChatbotEngine;
import com.campusscheduler.ai.models.User;
import com.campusscheduler.ai.utils.LocalAuthManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class StudentChatActivity extends AppCompatActivity {

    public static class ChatMessage implements Serializable {
        public String sender;
        public String message;
        public ChatMessage(String sender, String message) {
            this.sender = sender;
            this.message = message;
        }
    }

    private ImageButton btnBack, btnSendChat;
    private EditText etChatInput;
    private RecyclerView rvChat;
    private GenericAdapter<ChatMessage> adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_chat);

        currentUser = LocalAuthManager.getCurrentLocalUser(this);

        btnBack = findViewById(R.id.btnBack);
        btnSendChat = findViewById(R.id.btnSendChat);
        etChatInput = findViewById(R.id.etChatInput);
        rvChat = findViewById(R.id.rvChat);

        btnBack.setOnClickListener(v -> finish());
        btnSendChat.setOnClickListener(v -> sendMessage());

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericAdapter<>(messageList, (msg, holder) -> {
            holder.setText(R.id.tvTitle, msg.sender);
            holder.setText(R.id.tvSubtitle, msg.message);
        }, R.layout.item_generic);
        rvChat.setAdapter(adapter);

        messageList.add(new ChatMessage("CampusScheduler AI", "Hi " + (currentUser != null ? currentUser.getName() : "Student") + "! Ask me anything about your class timetable, room numbers, pending assignments, exams, or attendance."));
        adapter.notifyDataSetChanged();
    }

    private void sendMessage() {
        String prompt = etChatInput.getText().toString().trim();
        if (prompt.isEmpty()) return;

        messageList.add(new ChatMessage("You", prompt));
        etChatInput.setText("");
        adapter.notifyDataSetChanged();
        rvChat.smoothScrollToPosition(messageList.size() - 1);

        AcademicChatbotEngine.processQuery(prompt, currentUser, answerText -> {
            messageList.add(new ChatMessage("CampusScheduler AI", answerText));
            adapter.notifyDataSetChanged();
            rvChat.smoothScrollToPosition(messageList.size() - 1);
        });
    }
}
