package com.littlew.example;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 为了测试不同class但是同一个名称的引用
 */
public class BaseActivity extends AppCompatActivity {

    public int a = 0;

    protected int b = 1;

    private int c = 2;

    protected void doOverride() {

    }
}
