package pl.adid.pingurl

import android.content.Context
import android.content.SharedPreferences
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
    private var timeout: Int = 3000

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
                    timeoutInput.text.toString().toInt()
        saveInputUrl(url)
        hideKeyboard()
        if (!isSubscribed) {
            isSubscribed = true
            connectionAcquired = true
            pingUrl(URL("http", url, 80, ""), timeout)
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

    private fun pingUrl(url: URL, timeout: Int) {
        disposable +=
                Schedulers.io().schedulePeriodicallyDirect({
                    ping(url, timeout)
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
                                            is SocketTimeoutException -> statusPing.text = "SocketTimeout"
                                        }
                                    },
                                    {
                                        if (!connectionAcquired) {
                                            connectionAcquired = true
                                            tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                                        }
                                        statusPing.text = "$it ms"
                                        addItemToList(it)
                                        statusBackground
                                                .setBackgroundColor(ContextCompat.getColor(this, R.color.colorGreen))
                                    }
                            )
                }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun ping(url: URL, timeout: Int): Single<Int> {
        return Single.create { emitter ->
            try {
                val hostAddress: String = InetAddress.getByName(url.host).hostAddress
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress(hostAddress, url.port), timeout)
                socket.close()
                val probeFinish = System.currentTimeMillis()
                emitter.onSuccess((probeFinish - start).toInt())
            } catch (e: Exception) {
                e.printStackTrace()
                emitter.onError(e)
            }
        }
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
