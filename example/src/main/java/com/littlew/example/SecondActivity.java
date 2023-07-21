package com.littlew.example;

import static com.littlew.example.pb.ObTest.InnerClass.OV_VALUE_INNER;

import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.littlew.example.pa.DataA;
import com.littlew.example.pb.ObTest.InnerClass;
import com.littlew.example.pf.ia.D;

public class SecondActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        i();
        new ExClass().e();

        D d = new D();

        //比较与MainActivity中的使用形式
        String f = InnerClass.OV_VALUE_INNER;

        String g = OV_VALUE_INNER;
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