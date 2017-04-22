package eu.the4thfloor.msync.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_KEY
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_REDIRECT_URI
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_SECRET
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.api.MeetupApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

class LoginActivity : Activity() {

    @Inject lateinit var api: MeetupApi

    private val disposables = CompositeDisposable()
    private var webView: WebView? = null
    private var state: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        MSyncApp.graph.inject(this)

        webView = WebView(this)
        webView?.let {

            // it.settings.javaScriptEnabled = true

            it.setWebViewClient(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    MyWebViewClientNougat(this)
                                } else {
                                    MyWebViewClient(this)
                                })
            setContentView(it)

            state = Uri.encode(state())
            val url = Uri.Builder()
                .scheme("https")
                .authority("secure.meetup.com")
                .appendPath("oauth2")
                .appendPath("authorize")
                .appendQueryParameter("client_id", MEETUP_OAUTH_KEY)
                .appendQueryParameter("redirect_uri", MEETUP_OAUTH_REDIRECT_URI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "rsvp")
                .appendQueryParameter("set_mobile", "on")
                .appendQueryParameter("state", state)
                .build().toString()

            Timber.i("request: %s", url)
            it.loadUrl(url)
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() ?: false)
            webView?.goBack()
        else
            super.onBackPressed()
    }

    private fun handleUri(uri: Uri): Boolean {

        Timber.i("uri: %s", uri)
        if (!uri.toString().startsWith(MEETUP_OAUTH_REDIRECT_URI)) {
            return false
        }

        if (uri.getQueryParameter("state") != state) {
            // TODO state is wrong
            return false
        }

        return uri.getQueryParameter("code")?.let {
            loadToken(it)
            true
        } ?: false
    }

    private fun loadToken(code: String) {
        disposables.add(
            api
                .access(mapOf("client_id" to MEETUP_OAUTH_KEY,
                              "client_secret" to MEETUP_OAUTH_SECRET,
                              "redirect_uri" to MEETUP_OAUTH_REDIRECT_URI,
                              "code" to code,
                              "grant_type" to "authorization_code"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        response ->
                        Timber.i("isSuccessful: %s", response.isSuccessful)
                        Timber.i("access_token: %s", response.body().access_token)
                    },
                    {
                        Timber.e(it)
                    }))
    }

    private fun showError() {

    }

    fun state(): String =
        StringBuilder().apply {
            val generator = Random()
            for (i in 0..32) {
                append((generator.nextInt(93) + 33).toChar())
            }
        }.toString()

    private class MyWebViewClient(activity: LoginActivity,
                                  val ref: WeakReference<LoginActivity> = WeakReference(activity)) : WebViewClient() {


        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            ref.get()?.handleUri(Uri.parse(url)) ?: false
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class MyWebViewClientNougat(activity: LoginActivity,
                                        val ref: WeakReference<LoginActivity> = WeakReference(activity)) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            ref.get()?.handleUri(request.url) ?: false
    }
}
