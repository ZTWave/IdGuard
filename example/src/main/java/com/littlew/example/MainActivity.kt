package com.littlew.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.littlew.example.pa.DataA
import com.littlew.example.pb.ObTest

class MainActivity : AppCompatActivity() {

    private lateinit var fab: FloatingActionButton

    @BindView(R.id.textview)
    lateinit var textview: TextView

    @BindView(R.id.recycler_view)
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        fab = findViewById(R.id.fab)

        fab.setOnClickListener { view ->
            textview.text = "Hello Android"
        }

        val stringArray = this.resources.getStringArray(R.array.str_array)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = RAdapter(stringArray.toList())
        }

        CPTest.ptr()

        DataA().test()

        ObTest.OV_VALUE

        ObTest.obFun()
    }

    class RAdapter(private val list: List<String>) : RecyclerView.Adapter<RVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RVH {
            return RVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_recycler_view, parent, false)
            )
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: RVH, position: Int) {
            holder.textView.text = list[position]
        }

    }

    class RVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView by lazy { itemView.findViewById(R.id.item_text) }
    }
}