package com.example.designerdnanet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;

public class PhoneAuthenticationActivity extends AppCompatActivity {
    private ViewModel viewModel;
    private Callback callback;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks onVerificationStateChangedCallbacks;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private EditText etVerificationCode;
    private EditText etMobileNumber;
    private String mobileNumber;
    private String verificationCode;
    private Button buttonRequestVerificationCode;
    private Button buttonVerifyUser;
    private Button buttonGoogleVerificationRequest;
    private ProgressBar progressBar;
    private boolean isRequested = false;
    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 123;
    private String userId;
    private static boolean isLoggedIn = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_phone_authentication);
        firebaseAuth = FirebaseAuth.getInstance();

        // UI Initialization
        buttonRequestVerificationCode = findViewById(R.id.btn_auth_request);
        buttonGoogleVerificationRequest = findViewById(R.id.btn_google_verification_request);
        final TextView textViewRequestNewVerificationCode = findViewById(R.id.tv_retry_auth);
        buttonVerifyUser = findViewById(R.id.buttonAuthorizedUser);
        etMobileNumber = findViewById(R.id.et_mobile);
        etVerificationCode = findViewById(R.id.et_enter_code);
        progressBar = findViewById(R.id.progressBar);
        String email =  "";
        String password = "";

        // Ask permissions
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.FOREGROUND_SERVICE
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this,PERMISSIONS, PERMISSION_ALL);
        }

        // Google Verification
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Phone Verification
        etMobileNumber.addTextChangedListener(textChangeWatcher(etMobileNumber));
        etVerificationCode.addTextChangedListener(textChangeWatcher(etVerificationCode));

        buttonRequestVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                loadMainActivity();
                mobileNumber = "+1" + etMobileNumber.getText().toString();
                sendVerificationCode(mobileNumber);
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        buttonVerifyUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRequested) {
                    progressBar.setVisibility(View.VISIBLE);
                    verificationCode = etVerificationCode.getText().toString();
                    verifyVerificationCode(verificationCode);
                } else {
                    showToastMessage("Please request verification code first.");
                }
            }
        });

        buttonGoogleVerificationRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

//        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(@NonNull Task<AuthResult> task) {
//                if (task.isSuccessful()) {
//                    // Sign in success, update UI with the signed-in user's information
//                    System.out.println("***** createUserWithEmail was successful *****");
//                    FirebaseUser user = firebaseAuth.getCurrentUser();
////                    Intent intent = new Intent(PhoneAuthenticationActivity.this, MainActivity.class);
////                    startActivity(intent);
//                } else {
//                    // If sign in fails, display a message to the user/
//                    System.out.println("***** createUserWithEmail failed: " + task.getException() + " *****");
//                    showToastMessage("Authentication failed");
//                }
//            }
//        });

//        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//            @Override
//            public void onComplete(@NonNull Task<AuthResult> task) {
//                if (task.isSuccessful()) {
//                    // Sign in success, update UI with the signed-in user's information
//                    System.out.println("***** signInWithEmail was successful *****");
//                    FirebaseUser user = firebaseAuth.getCurrentUser();
//                    Intent intent = new Intent(PhoneAuthenticationActivity.this, MainActivity.class);
//                    startActivity(intent);
//                } else {
//                    // If sign in fails, display a message to the user.
//                    System.out.println("***** signInWithEmail failed: " + task.getException() + " *****");
//                    showToastMessage("Authentication Failed");
//                    // ...
//                }
//            }
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        if (isLoggedIn) {
            System.out.println("***** isloggedin1 : " + isLoggedIn + " *****");
            isLoggedIn = getIntent().getBooleanExtra("isLoggedIn", true);
            System.out.println("***** isloggedin2 : " + isLoggedIn + " *****");
            if (isLoggedIn) {
                currentUser = firebaseAuth.getCurrentUser();
                System.out.println("***** currentuser : " + currentUser + " *****");
                if (currentUser != null) {
                    System.out.println("***** User is already signed in *****");
                    loadMainActivity();
                }
            } else {
                currentUser = null;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
//        if (currentUser != null) {
//            System.out.println("***** User is already signed in *****");
//            loadMainActivity();
//        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public TextWatcher textChangeWatcher(final EditText editText) {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editText.getId() == R.id.et_mobile && editText.getText().length() > 9) {
                    buttonRequestVerificationCode.setBackground(ContextCompat.getDrawable(PhoneAuthenticationActivity.this, R.drawable.bg_btn_solid_orange_f79256_radius_8dp));
                    buttonRequestVerificationCode.setTextColor(ContextCompat.getColor(PhoneAuthenticationActivity.this, R.color.colorWhiteFDFD));
                    buttonRequestVerificationCode.setEnabled(true);
                }
                if (editText.getId() == R.id.et_enter_code && editText.getText().length() > 5) {
                    buttonVerifyUser.setBackground(ContextCompat.getDrawable(PhoneAuthenticationActivity.this, R.drawable.bg_btn_solid_orange_f79256_radius_8dp));
                    buttonVerifyUser.setTextColor(ContextCompat.getColor(PhoneAuthenticationActivity.this, R.color.colorWhiteFDFD));
                    buttonVerifyUser.setEnabled(true);
                }
            }
        };
        return textWatcher;
    }

    private void loadMainActivity() {
        Intent intent = new Intent(PhoneAuthenticationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (currentUser!=null) {
            userId = currentUser.getUid();
        } else {
            userId = firebaseAuth.getUid();
        }
        System.out.println("!!!!! firebase auth getuid: " + userId);
        intent.putExtra("UID", userId);
        startActivity(intent);
    }

    private void sendVerificationCode(String mobile) {
        System.out.println("***** mobile number: " + mobile + " *****");
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(mobile)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(verificationCallback)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.

            //Getting the code sent by SMS
            isRequested = true;
            progressBar.setVisibility(View.GONE);
            String code = phoneAuthCredential.getSmsCode();

            //sometime the code is not detected automatically
            //in this case the code will be null
            //so user has to manually enter the code
            if (code != null && isRequested) {
                etVerificationCode.setText(code);
                //verifying the code
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            isRequested = false;
            progressBar.setVisibility(View.GONE);
            System.out.println("***** Verification failed: " + e + " *****");
            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                showToastMessage("Invalid request. Please enter valid phone number");
                System.out.println("***** Invalid request: " + e.toString() + " *****");
            } else if (e instanceof FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                showToastMessage("SMS quota has exceeded");
            }

        }

        @Override
        public void onCodeSent(@NonNull String verificationId,
                               @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            super.onCodeSent(verificationId, forceResendingToken);
            System.out.println("***** onCodeSent: " + verificationId + "*****");
            isRequested = true;
            progressBar.setVisibility(View.GONE);
            mVerificationId = verificationId;
            resendToken = forceResendingToken;
        }
    };

    private void showToastMessage (String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void verifyVerificationCode(String code) {
        //creating the credential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        //signing the user
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(PhoneAuthenticationActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            isLoggedIn = true;
                            //verification successful we will start the profile activity
                            loadMainActivity();
                            createFirebaseUserDocument();
                        } else {
                            //verification unsuccessful.. display an error message
                            String message = "Somthing is wrong, we will fix it soon...";
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered. Please try again";
                            }

                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            try {
                                View view = getCurrentFocus();
                                imm.hideSoftInputFromWindow(view.getWindowToken(),0);
                            } catch (Exception e) {
                                System.out.println("***** Keyboard is closed *****");
                            }
                            progressBar.setVisibility(View.GONE);
                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
                            snackbar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            });
                            snackbar.show();
                        }
                    }
                });
    }

    private void createFirebaseUserDocument() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            userId = firebaseAuth.getUid();
            if (userId == null) {
                userId = firebaseAuth.getCurrentUser().getUid();
            }
        } catch (Exception e) {
            System.out.println("*****  user document create failed *****");
        }
        DocumentReference documentReference = db.collection("users").document(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("UID", userId);
        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                System.out.println("***** user profile is created *****");
            }
        });
    }

    // Google Verification
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                System.out.println("***** Google Sign In Failed: " + e.toString() + " *****");
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            try {
                                userId = firebaseAuth.getUid();
                                if (userId == null) {
                                    userId = firebaseAuth.getCurrentUser().getUid();
                                }
                            } catch (Exception e) {
                                System.out.println("*****  user document create failed *****");
                            }
                            loadMainActivity();
                        } else {
                            // If sign in fails, display a message to the user.



                        }

                        // ...
                    }
                });
    }



}
