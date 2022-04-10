package com.versekeeper.apis;

import com.versekeeper.models.BibleAPI.API_PassageList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface API_Bible {

    // Encoding Source: https://stackoverflow.com/a/35361466/848353
    @GET("{versionIdAndPassageIdPath}")
    Call<API_PassageList> getPassages(@Path(value = "versionIdAndPassageIdPath", encoded = true) String versionIdAndPassageIdPath,
                                      @Query("fums-version") String fumsVersion);

    // Encoding Source: https://stackoverflow.com/a/35361466/848353
    // Void Source: https://stackoverflow.com/a/33228322/848353
    @GET("{fumsPath}")
    Call<Void> sendFums(@Path(value = "fumsPath", encoded = true) String fumsPath,
                                @Query("t") String fumsId,
                                @Query("dId") String fbId,
                                @Query("sId") String fbSessionId);
}