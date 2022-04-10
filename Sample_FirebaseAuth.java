package com.versekeeper;

import static androidx.navigation.Navigation.findNavController;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.versekeeper.utilities.PrefList;

import java.util.UUID;

public class Account extends Fragment {

    private static final String TAG = "Account";
    private static final int RC_SIGN_IN = 9001;


    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    // Store and Access shared preferences
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefEditor;

    // Views
    private CardView cvGSignIn;
    private CardView cvGSignOut;
    private ImageView ivGSignOut;
    private CardView cvFBaseSignIn;
    private CardView cvAbout;


    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag_account, container, false);

        // Make sure this is before calling super.onCreate
        // Source: https://medium.com/android-news/the-complete-android-splash-screen-guide-c7db82bce565
        requireActivity().setTheme(R.style.AppTheme);

        prefs = requireActivity().getApplicationContext().getSharedPreferences(PrefList.ids, Context.MODE_PRIVATE);


        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();


        // Google SignIn
        cvGSignIn = view.findViewById(R.id.account_gSignIn_cardView);
        cvGSignIn.setOnClickListener(v -> {
            signIn();
        });

        // Google SignOut
        cvGSignOut = view.findViewById(R.id.account_gSignOut_cardView);
        cvGSignOut.setOnClickListener(v -> {
            // Sign out
            signOut();
            updateUI(null);
        });
        ivGSignOut = view.findViewById(R.id.account_gSignOut_imageView);

        // Firebase SignIn
        cvFBaseSignIn = view.findViewById(R.id.account_fbaseSignIn_cardView);

        NavController navController = findNavController(requireActivity(), R.id.nav_host_fragment);
        // Settings
        // Load settings fragment when user clicks on settings CardView
        CardView cvSettings = view.findViewById(R.id.account_settings_cardView);
        cvSettings.setOnClickListener(v -> {
            // Load the Settings fragment by the navController since the fragment is not
            // part of the bottom navigation, which is limited to 5 items per Material design specs
            navController.navigate(R.id.settings_navigation);
        });

        // About
        CardView cvAbout = view.findViewById(R.id.account_about_cardView);
        cvAbout.setOnClickListener(v -> {
            navController.navigate(R.id.about_navigation);
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();


        Bundle bundle = getArguments();
        if (bundle != null && bundle.getBoolean("profileClick")) {
            // Notify activity ("profileClick") the user chose this action to prevent
            // the activity from auto navigating back to the Home fragment
            manageSignInStatus(currentUser);
            return;
        }
        // Check if user is signed in (non-null) and update UI accordingly.
        updateUI(currentUser);
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
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }


    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    public void signOut() {
        googleSignInClient.signOut();
        FirebaseAuth.getInstance().signOut();
    }

    private void manageSignInStatus(FirebaseUser user) {
        // Hide sign in/out cardViews depending on the sign-in state
        if (user == null) {
            // Signed Out
            cvGSignIn.setVisibility(View.VISIBLE);

            cvGSignOut.setVisibility(View.INVISIBLE);
        } else {
            // User is already Signed In
            // Hide Sign ins
            cvGSignIn.setVisibility(View.GONE);
            cvFBaseSignIn.setVisibility(View.GONE);
            // Show Sign out
            cvGSignOut.setVisibility(View.VISIBLE);


            // Load user's profile pic. Source: https://stackoverflow.com/a/37116931/848353
            // Rounded Corners Source: https://stackoverflow.com/a/32390715/848353
            Glide.with(requireActivity()).load(user.getPhotoUrl()).asBitmap()
                    .centerCrop().into(new SimpleTarget<Bitmap>(100, 100) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(requireActivity().getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    ivGSignOut.setBackground(circularBitmapDrawable);
                }
            });
        }
    }

    private void updateUI(FirebaseUser user) {
        // Hide sign in/out cardViews depending on the sign-in state
        if (user == null) {
            // Signed Out
            cvGSignIn.setVisibility(View.VISIBLE);
            cvFBaseSignIn.setVisibility(View.VISIBLE);
            cvGSignOut.setVisibility(View.INVISIBLE);
        } else {
            // Signed In
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    if (idToken != null) {

                        // Store in shared preferences
                        prefEditor = prefs.edit();
                        prefEditor.putString("fbSessionId", idToken);
                        prefEditor.apply();

                        // Used to provide session id to API.Bible without submitting Firebase tokens to third-parties
                        prefEditor = prefs.edit();
                        prefEditor.putString("customSessionId", UUID.randomUUID().toString());
                        prefEditor.apply();
                    }
                }
            });

            // Navigate to the home fragment
            NavController navController = findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.home_navigation);
            String welcomeMsg = user.getDisplayName() + " " + getString(R.string.signedIn);
            Toast.makeText(requireActivity(), welcomeMsg, Toast.LENGTH_SHORT).show();
        }
    }


}