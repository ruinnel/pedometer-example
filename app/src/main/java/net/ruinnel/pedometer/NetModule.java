/*
 * Filename	: NetModule.java
 * Function	:
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.app.Application;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import net.ruinnel.pedometer.api.NaverMapClient;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Module
public class NetModule {
  private static final String TAG = NetModule.class.getSimpleName();

  public interface RequestModifiedListener {
    void requestModified(Request request);
  }

  private RequestModifiedListener mRequestModifiedListener;

  // Constructor needs one parameter to instantiate.
  public NetModule(RequestModifiedListener listener) {
    mRequestModifiedListener = listener;
  }

  @Provides
  @Singleton
  Cache provideOkHttpCache(Application application) {
    int cacheSize = 10 * 1024 * 1024; // 10 MiB
    Cache cache = new Cache(application.getCacheDir(), cacheSize);
    return cache;
  }

  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient(final Settings settings) {
    HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
    httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.BASIC);

    // add default headers
    Interceptor headerInterceptor = new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request request = null;

        if (request == null) {
          request = original.newBuilder()
            .header("User-Agent", settings.getUserAgent())
            .header("X-Naver-Client-Id", settings.getNaverClientId())
            .header("X-Naver-Client-Secret", settings.getNaverClientSecret())
            .header("Content-Type", "application/json")
            .method(original.method(), original.body())
            .build();
        }

        if (mRequestModifiedListener != null) {
          mRequestModifiedListener.requestModified(request);
        }

        return chain.proceed(request);
      }
    };

    OkHttpClient client = new OkHttpClient.Builder()
      .addInterceptor(httpLoggingInterceptor)
      .addInterceptor(headerInterceptor)
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .build();

    return client;
  }

  @Provides
  @Singleton
  Retrofit provideRetrofit(Settings settings, Gson gson, OkHttpClient client) {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(settings.getNaverBaseURL())
      .client(client)
      .addConverterFactory(GsonConverterFactory.create(gson))
      .build();
    return retrofit;
  }

  @Provides
  @Singleton
  NaverMapClient provideNaverMapClient(Retrofit retrofit) {
    return retrofit.create(NaverMapClient.class);
  }
}
