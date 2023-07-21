package com.littlew.example;

import static com.littlew.example.pb.ObTest.OV_IM_VALUE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.littlew.example.base.BaseActivity;
import com.littlew.example.pa.DataA;
import com.littlew.example.pb.ObTest;
import com.littlew.example.pc.ATxtCusView;
import com.littlew.example.pd.OInterface;
import com.littlew.example.pd.OtInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements OInterface, OtInterface, AInterface {

    private FloatingActionButton fab;

    @BindView(R.id.textview)
    TextView textview;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @BindView(R.id.donine_text)
    ATxtCusView textView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textview.setText("Hello Android");
            }
        });

        String[] stringArray = getResources().getStringArray(R.array.str_array);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new RAdapter(Arrays.asList(stringArray)));

        new DataA().test();

        String i = ObTest.OV_VALUE;

        String j = OV_IM_VALUE;

        String k = ObTest.InnerClass.OV_VALUE_INNER;

        ObTest.obfun();

        new SecondActivity().i();

        new SecondActivity.Inner();

        int a1 = a;

        int b1 = b;

        com.littlew.example.pf.ia.C c = new com.littlew.example.pf.ia.C();


    }

    @Override
    protected void doOverride() {
        super.doOverride();
    }

    @Override
    public void o() {
        //do something
    }

    @Override
    public void f() {
        //do something
    }

    void d() {

    }

    //类的签名一致 报错
    /*private void onCreate(@Nullable Bundle savedInstanceState){

    }*/

    static class Inner {
        private void i() {

        }
    }

    class RAdapter extends RecyclerView.Adapter<RVH> {

        private ArrayList<String> list = new ArrayList<String>();

        public RAdapter(List<String> list) {
            this.list.addAll(list);
        }

        @NonNull
        @Override
        public RVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RVH(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_recycler_view, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull RVH holder, int position) {
            holder.textView.setText(list.get(position).toString());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    class RVH extends RecyclerView.ViewHolder {
        public TextView textView;

        public RVH(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.item_text);
        }
    }
}
