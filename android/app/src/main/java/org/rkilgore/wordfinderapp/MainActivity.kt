package org.rkilgore.wordfinderapp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView

import org.rkilgore.wordfinder.WordFinder
import org.rkilgore.wordfinder.WordInfo
import java.util.*
import kotlin.Comparator

class MainActivity : AppCompatActivity(), View.OnKeyListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val spinner=findViewById<ProgressBar>(R.id.progressBar)
        spinner.visibility = View.GONE
        val lettersText = findViewById<EditText>(R.id.lettersText)
        val patternText = findViewById<EditText>(R.id.patternText)
        lettersText.setOnKeyListener(this)
        patternText.setOnKeyListener(this)
        lettersText.requestFocus()

        val wordsFile = Scanner(assets.open("scrabble_words.txt"))
        this.wf = WordFinder(wordsFile)
    }

    fun sendMessage(view: View) {
        hideKeyboard()
        findWords(view)
    }

    private fun hideKeyboard() {
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
        Thread.sleep(250)
    }

    private fun findWords(view: View) {
        val lettersText = findViewById<EditText>(R.id.lettersText)
        val patternText = findViewById<EditText>(R.id.patternText)
        val letters = lettersText.text.toString()
        val pattern = patternText.text.toString()
        // println(String.format("rkilgore: letters=%s  pattern=%s", letters, pattern))
        val output = findViewById<TextView>(R.id.outputText)
        output.text = ""
        output.invalidate()

        val spinner=findViewById<ProgressBar>(R.id.progressBar)
        spinner.visibility = View.VISIBLE

        val thread = Thread {
            val map = wf!!.findWords(letters, pattern, 7, 7)

            val words = ArrayList<String>(map.keys)
            words.sortWith(Comparator { a: String, b: String ->
                run {
                    val ainf = map[a]!!
                    val binf = map[b]!!
                    if (ainf.score != binf.score) {
                        return@run binf.score - ainf.score
                    }
                    if (a.length != b.length) {
                        return@run b.length - a.length
                    }
                    if (ainf.dotVals.length != ainf.dotVals.length) {
                        return@run ainf.dotVals.length - binf.dotVals.length
                    }
                    if (ainf.dotVals != binf.dotVals) {
                        return@run ainf.dotVals.compareTo(binf.dotVals)
                    }
                    return@run a.compareTo(b);
                }
            })

            val sb = StringBuilder()
            for (word in words) {
                val winf: WordInfo = map[word]!!
                val dotVals: String = winf.dotVals
                val dots: String = if (dotVals.isNotEmpty()) "$dotVals: " else ""
                sb.append(String.format("%s%s score: %d\n", dots, word, winf.score))
            }
            runOnUiThread {
                spinner.visibility = View.GONE
                output.text = sb.toString()
            }
        }
        thread.start()
    }

    override fun onKey(view: View, keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in listOf(IME_ACTION_DONE, IME_ACTION_GO, KeyEvent.KEYCODE_ENTER)) {
            hideKeyboard()
            findWords(view)
        }
        return true
    }

    private var wf: WordFinder? = null
}
