package com.example.designerdnanet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.designerdnanet.SendNotificationPack.APIService;
import com.example.designerdnanet.SendNotificationPack.Client;
import com.example.designerdnanet.SendNotificationPack.Data;
import com.example.designerdnanet.SendNotificationPack.MyResponse;
import com.example.designerdnanet.SendNotificationPack.NotificationSender;
import com.example.designerdnanet.SendNotificationPack.Token;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.installations.InstallationTokenResult;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendNotifications extends AppCompatActivity {
    private APIService apiService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);
        Button send = findViewById(R.id.buttonStopTest);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseDatabase.getInstance().getReference().child("Tokens").child("").child("token").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String userToken = snapshot.getValue(String.class);
                        sendNotifications(userToken, "test title", "test message");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
        updateToken();
    }

    private void updateToken() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        Task<InstallationTokenResult> task = FirebaseInstallations.getInstance().getToken(true);
        String refreshToken = task.getResult().getToken();
        Token token = new Token(refreshToken);
        FirebaseDatabase.getInstance().getReference("Tokens").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
    }

    private void sendNotifications(String userToken, String title, String message) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, userToken);
        apiService.sendNotifcation(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    try {
                        if (response.body().success != 1) {
                            Toast.makeText(SendNotifications.this, "Failed", Toast.LENGTH_LONG);
                        }
                    } catch (Exception e) {
                        System.out.println("***** OnResponse Error: " + e + " *****");
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {

            }
        });
    }
}
