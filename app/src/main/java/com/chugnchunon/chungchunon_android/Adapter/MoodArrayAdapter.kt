package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.R
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_my_diary.view.*
import kotlinx.android.synthetic.main.item_spinner.view.*

class MoodArrayAdapter(ctx: Context, moods: List<Mood>) : ArrayAdapter<Mood>(ctx, 0, moods) {
    override fun getView(position: Int, recycledView: View?, parent: ViewGroup): View {
        return this.createView(position, recycledView, parent)
    }

    override fun getDropDownView(position: Int, recycledView: View?, parent: ViewGroup): View {
        return this.createView(position, recycledView, parent)
    }

    private fun createView(position: Int, recycledView: View?, parent: ViewGroup): View {
        val mood = getItem(position)

        val view = recycledView ?: LayoutInflater.from(context).inflate(
            R.layout.item_spinner, parent, false
        )

        if (mood != null) {
            view.moodImage.setImageResource(mood.image)
            view.moodText.text = mood.description
        }

        return view
    }
}