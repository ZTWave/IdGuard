package com.littlew.example;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.littlew.example.pa.DataA;

public class SecondActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        i();
        new ExClass().e();
    }

    //public
    public void i() {

    }

    private void j(String k) {

    }

    protected void p() {

    }

    static void l() {

    }

    native void n();

    private String f() {
        return "";
    }

    private DataA o() {
        return new DataA();
    }

    static class Inner {
        private void i() {

        }
    }

    //can not use from other class
    class Inner2 {
        private void j() {

        }
    }

}

class ExClass {
    public void e() {

    }
}