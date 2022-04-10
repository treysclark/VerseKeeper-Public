package com.versekeeper.persistence;

import android.app.Activity;
import android.app.Application;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.versekeeper.R;
import com.versekeeper.apis.API_Bible;
import com.versekeeper.models.BibleAPI.API_PassageList;
import com.versekeeper.models.CategoryCounts;
import com.versekeeper.models.KJV;
import com.versekeeper.models.Label;
import com.versekeeper.models.LabelVerse;
import com.versekeeper.models.Note;
import com.versekeeper.models.Reference;
import com.versekeeper.models.Schedule;
import com.versekeeper.models.SnackbarSettings;
import com.versekeeper.models.Verse;
import com.versekeeper.models.VerseLabel;
import com.versekeeper.models.VerseLabel_Filter;
import com.versekeeper.models.VerseLabel_Join;
import com.versekeeper.models.Version;
import com.versekeeper.utilities.GetSnackbar;
import com.versekeeper.utilities.UtilQueryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppRepository {
    private static final String TAG = "AppRepository";

    // Vars: API_Bible
    private static final String BIBLE_BASE_URL = "https://api.scripture.api.bible/v1/";
    private static final String FUMS_BASE_URL = "https://fums.api.bible/";
    private static final String BIBLE_API_KEY = "8f8746f92e835d6b4c279ff6195305c2";
    private final API_Bible apiBiblePassage;
    private final API_Bible apiBibleFums;
    private final MutableLiveData<API_PassageList> passageList = new MutableLiveData<>();
    private final MutableLiveData<String> errorNotification = new MutableLiveData<>("");

    // Vars: Room
    ...

    // Constructor
    public AppRepository(Application application) {

        // --------API_BIBLE-------------//

        // Source: https://stackoverflow.com/a/50952871/848353
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .header("api-key", BIBLE_API_KEY)
                    .method(original.method(), original.body())
                    .build();
             return chain.proceed(request);
        });
        OkHttpClient client = httpClient.build();

        apiBiblePassage = new retrofit2.Retrofit.Builder()
                .baseUrl(BIBLE_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(API_Bible.class);

        OkHttpClient.Builder httpFumsClient = new OkHttpClient.Builder();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpFumsClient.interceptors().add(interceptor);
        httpFumsClient.addInterceptor(chain -> {
            Request original = chain.request();
            Request request = original.newBuilder()
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });
        OkHttpClient fumsClient = httpFumsClient.build();

        // Source: https://stackoverflow.com/a/40013869/848353
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        apiBibleFums = new retrofit2.Retrofit.Builder()
                .baseUrl(FUMS_BASE_URL)
                .client(fumsClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(API_Bible.class);

        // ------Room----------------//
        ...
    }

    // Getters & Setters: API
    public LiveData<API_PassageList> getPassageList() {
        return passageList;
    }

    public void setPassageList(String versionId, String passageId, String fbSessionId) {

        String url = "bibles/" + versionId + "/passages/" + passageId;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        apiBiblePassage.getPassages(url, "3").enqueue(new Callback<API_PassageList>() {
            @Override
            public void onResponse(@NonNull Call<API_PassageList> call, @NonNull Response<API_PassageList> response) {
                if (response.isSuccessful()) {
               //     Log.e("AppRepository", "setPassageList: Bible API call successful");
                    passageList.postValue(response.body());
                    // Report FumsToken & anonymous sessionId to API.Bible
                    reportFums(response, currentUser, fbSessionId);

                } else {
                    Log.e("AppRepository", "setPassageList: Bible API call unsuccessful");
                    passageList.postValue(null);
                    if (response.code() == 404) {
                        errorNotification.postValue("Oh-no, there was a problem./n/n Please try a different Bible version or copy and paste the passage.");
                        Log.e(TAG, "onResponse: 404 and Url:" + url);
                    } else {
                        Log.e(TAG, "Import was not successful (" + response.code() + ")" + " - url: " + url);
                        errorNotification.postValue("Import was not successful (" + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<API_PassageList> call, @NonNull Throwable t) {
                Log.e("AppRepository", "setPassageList: Bible API call failed");
                passageList.postValue(null);
                errorNotification.postValue("Import Failure:\n\n" + "Error message: " + t.getMessage());
            }
        });
    }

    // Report FumsToken to API.Bible
    private void reportFums(@NonNull Response<API_PassageList> response, FirebaseUser currentUser, String fbSessionId) {
        if (response.body() == null || currentUser == null) {
            return;
        }
        String fumsToken = response.body().fumsToken.fumsToken;
        String fumsUrl = "/f3";
        String fbId = currentUser.getUid();

        apiBibleFums.sendFums(fumsUrl, fumsToken, fbId, fbSessionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
       //         Log.e("AppRepository", "FUMS successful");
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {
                Log.e("AppRepository", "reportFums failed -> "+ throwable.getMessage() );
            }
        });
    }

    public MutableLiveData<String> getErrorNotification() {
        return errorNotification;
    }


    //---------------Room--------------------------------------//
    ...