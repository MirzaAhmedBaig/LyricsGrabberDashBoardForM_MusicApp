package org.mab.m_musicdashboard

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import org.jsoup.Jsoup


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val mDatabase by lazy {
        FirebaseDatabase.getInstance().reference
    }
    private var list = ArrayList<Lyrics>()
    private var nameList: List<String> = ArrayList()

    private var adapter: ArrayAdapter<String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListeners()
    }


    private fun setListeners() {
        update_songs.isEnabled = false
        songs_list.isEnabled = false
        mDatabase.child("lyrics").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    it.getValue<Lyrics>(Lyrics::class.java)?.let {
                        list.add(it)
                    }
                }
                Log.d(TAG, "List Size : ${list.size}")
                getLyrics(list[0])
                setListView()
                progress_bar.visibility = View.GONE
                update_songs.isEnabled = true
                songs_list.isEnabled = true
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "loadPost:onCancelled", databaseError.toException())

            }

        })
        update_songs.setOnClickListener {
            updateLyricsUrls()
        }
    }

    private fun setListView() {
        nameList = list.map { it.title }
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, nameList)
        songs_list.adapter = adapter
    }

    private fun updateLyricsUrls() {

        progress_bar.visibility = View.VISIBLE
        update_songs.isEnabled = false
        songs_list.isEnabled = false
        Thread(Runnable {
            ('a'..'z').forEach {
                try {
                    val doc = Jsoup.connect("$BASE_URL/lyrics/$it").get()
                    val mainDiv = doc.select("fieldset").first()
                    val artistLinks = mainDiv.select("a[href]")
                    for (link in artistLinks) {
                        val artist = link.text().replace(" lyrics", "")
                        try {
                            val songListDoc = Jsoup.connect(BASE_URL + link.attr("href")).get()
                            val songListDiv = songListDoc.select("div.all-lyrics.top-lyrics").first()
                            val songsLinks = songListDiv.select("a[href]")
                            for (e in songsLinks) {
                                Log.d(TAG, "Link : " + BASE_URL + e.attr("href"))
                                val data = Lyrics()
                                data.title = e.text()
                                data.artists = artist
                                data.url = BASE_URL + e.attr("href")
                                list.add(data)
                                Log.d(TAG, "Artist : $artist")
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Error : " + e.message)
                        }


                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error : " + e.message)
                }
            }
            runOnUiThread {
                val distinctList = list.distinctBy { it -> it.title }
                Log.d(TAG, "Done List Size : ${distinctList.size}")
                setListView()
                distinctList.forEach {
                    val key = mDatabase.child("lyrics").push().key
                    mDatabase.child("lyrics").child(key!!).setValue(it)
                }
                progress_bar.visibility = View.GONE
                update_songs.isEnabled = true
                songs_list.isEnabled = true
            }
        }).start()
    }

    private fun getLyricsURL(info: Lyrics): Lyrics? {
        val artistList = list.filter { it.artists == info.artists }
        return if (artistList.isNotEmpty()) {
            val list = artistList.filter { it.title == info.title }
            if (list.isNotEmpty())
                list[0]
            else
                null
        } else {
            null
        }
    }

    private fun getLyrics(info: Lyrics) {
        Thread(Runnable {
            try {
                Log.d(TAG, "URL : ${info.url}")
                val doc = Jsoup.connect(info.url).get()
                val lyricsData = doc.select("p#lyrics_text")
                Log.d(TAG, "Lyrics for ${info.title} : ${lyricsData.text()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()

    }


}
