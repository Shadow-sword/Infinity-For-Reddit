package ml.docilealligator.infinityforreddit.apis;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

import java.util.Map;

public interface VolcanoEngineAPI {

    @POST("api/v3/responses")
    Call<String> translate(@HeaderMap Map<String, String> headers, @Body Map<String, Object> body);
}
