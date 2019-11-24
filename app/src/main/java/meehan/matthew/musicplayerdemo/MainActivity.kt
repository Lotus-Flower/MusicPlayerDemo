package meehan.matthew.musicplayerdemo

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction().replace(R.id.main_activity_frame_layout, SongListFragment()).addToBackStack(null).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.main_activity_frame_layout, SongListFragment()).commit()
        return super.onOptionsItemSelected(item)
    }
}
