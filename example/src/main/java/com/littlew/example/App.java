package com.littlew.example;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    static class SPHolder {
        SharedPreferences sp;

        SPHolder(Context c) {
            sp = c.getSharedPreferences("sp", Context.MODE_PRIVATE);
        }
    }
}
