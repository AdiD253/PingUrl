package pl.adid.pingurl

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import pl.adid.pingurl.adapter.PingListAdapter
import pl.adid.pingurl.api.Api
import java.lang.Exception
import java.net.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    val URL = "inputUrl"

    private var menu: Menu? = null

    private val disposable = CompositeDisposable()
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private lateinit var sharedPrefs: SharedPreferences
    private var isSubscribed = false
    private var connectionAcquired = true
    private var timeout: Long = 3000

    private val pingListAdapter: PingListAdapter by lazy {
        PingListAdapter()
    }
    private val linearLayoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        addressInput.setText(getInputUrl())
        initRecycler()
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_icons, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.iconRun -> {
                item.isVisible = false
                val iconPause = menu?.findItem(R.id.iconPause)
                iconPause?.isVisible = true
                startChecking()
            }
            R.id.iconPause -> {
                item.isVisible = false
                val iconRun = menu?.findItem(R.id.iconRun)
                iconRun?.isVisible = true
                stopChecking()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initRecycler() {
        pingList.apply {
            layoutManager = linearLayoutManager
            adapter = pingListAdapter
        }
    }

    private fun startChecking() {
        val url = addressInput.text.toString().trim()
        this.timeout =
                if (timeoutInput.text.trim().isEmpty())
                    3000
                else
                    timeoutInput.text.toString().toLong()
        saveInputUrl(url)
        hideKeyboard()
        if (!isSubscribed) {
            isSubscribed = true
            connectionAcquired = true
            pingUrl(url, timeout)
        }
    }

    private fun stopChecking() {
        isSubscribed = false
        connectionAcquired = false
        disposable.clear()
        statusPing.text = ""
        pingListAdapter.clearItemList()
        statusBackground.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryLight))
    }

    private fun pingUrl(url: String, timeout: Long) {
        val api = Api(url, timeout)
        disposable +=
                Schedulers.io().schedulePeriodicallyDirect({
                    val startTime = System.currentTimeMillis().toInt()
                    api.getVersion()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeBy(
                                    {
                                        statusPing.text = ""
                                        statusBackground
                                                .setBackgroundColor(ContextCompat.getColor(this, R.color.colorRed))
                                        addItemToList(1)
                                        if (connectionAcquired) {
                                            connectionAcquired = false
                                            tone.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 1000)
                                        }
                                        when (it) {
                                            is SocketTimeoutException -> statusError.text = "Timeout"
                                            else -> statusError.text = it.message
                                        }
                                    },
                                    {
                                        if (!connectionAcquired) {
                                            connectionAcquired = true
                                            tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                                        }
                                        val endTime = System.currentTimeMillis().toInt()
                                        val responseTime = endTime - startTime
                                        statusPing.text = "$responseTime ms"
                                        statusError.text = ""
                                        addItemToList(responseTime)
                                        statusBackground
                                                .setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreen))
                                    }
                            )
                }, 0, 3000, TimeUnit.MILLISECONDS)
    }

    private fun saveInputUrl(url: String) {
        val editor = sharedPrefs.edit()
        editor.putString(URL, url)
        editor.apply()
    }

    private fun getInputUrl() = sharedPrefs.getString(URL, "")

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun addItemToList(item: Int) {
        val lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
        pingListAdapter.addItemTiList(item, lastVisiblePosition)
        pingList.scrollToPosition(0)
    }

}
