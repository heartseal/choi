/*
package com.example.Rainbow_Voca.util


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.Rainbow_Voca.model.Word
import com.example.Rainbow_Voca.R

class AllWordsAdapter(private val words: List<Word>) : RecyclerView.Adapter<AllWordsAdapter.WordViewHolder>() {

    class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textWord: TextView = itemView.findViewById(R.id.text_word)
        val textMeaning: TextView = itemView.findViewById(R.id.text_meaning)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = words[position]
        holder.textWord.text = word.word
        holder.textMeaning.text = word.meaning
    }

    override fun getItemCount(): Int = words.size
}
*/
