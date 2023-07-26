package com.littlew.example.base;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    public int a = 0;

    protected int b = 1;

    private int c = 2;

    protected void doOverride() {
        c = 4;
    }

    private void c() {
        int a = 2;
    }
}
