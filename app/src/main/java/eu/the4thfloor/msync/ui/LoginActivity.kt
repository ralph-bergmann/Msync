package eu.the4thfloor.msync.ui

import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_KEY
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_REDIRECT_URI
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_SECRET
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.models.AccessErrorResponse
import eu.the4thfloor.msync.api.models.AccessResponse
import eu.the4thfloor.msync.utils.PREF_ACCESS_TOKEN
import eu.the4thfloor.msync.utils.PREF_REFRESH_TOKEN
import eu.the4thfloor.msync.utils.apply
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.startActivity
import retrofit2.Retrofit
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

class LoginActivity : Activity() {

    @Inject lateinit var retrofit: Retrofit
    @Inject lateinit var api: MeetupApi

    private val disposables = CompositeDisposable()
    private val submitTransformer =
        { observable: Observable<SubmitEvent> ->
            observable
                .flatMap { (code) ->
                    api.access(mapOf("client_id" to MEETUP_OAUTH_KEY,
                                     "client_secret" to MEETUP_OAUTH_SECRET,
                                     "redirect_uri" to MEETUP_OAUTH_REDIRECT_URI,
                                     "code" to code,
                                     "grant_type" to "authorization_code"))
                        .map { response -> SubmitUiModel.success(response) }
                        .onErrorReturn { t ->
                            SubmitUiModel.failure(if (t is HttpException) {
                                                      retrofit
                                                          .responseBodyConverter<AccessErrorResponse>(AccessErrorResponse::class.java,
                                                                                                      arrayOfNulls<Annotation>(0))
                                                          .convert(t.response().errorBody())
                                                  } else {
                                                      null
                                                  })
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .startWith(SubmitUiModel.inProgress())
                }
        }

    private var state: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        MSyncApp.graph.inject(this)

        webView?.let {

            it.setWebViewClient(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    MyWebViewClientNougat(this)
                                } else {
                                    MyWebViewClient(this)
                                })

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

    private fun onPageStarted() {
        progressBar.visibility = View.VISIBLE
    }

    private fun onPageFinished() {
        progressBar.visibility = View.GONE
    }

    private fun loadToken(code: String) {
        disposables
            .add(Observable
                     .just(SubmitEvent(code))
                     .compose(submitTransformer)
                     .subscribe({ (inProgress, success, response, error) ->
                                    progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
                                    if (!inProgress) {
                                        if (success && response != null) {
                                            Timber.i("success %s", response.access_token)
                                            handleAccessResponse(response)
                                        } else {
                                            Timber.e("!success %s", error)
                                            showError()
                                        }
                                    }
                                },
                                { error ->
                                    Timber.e(error, "onCreate: ")
                                    showError()
                                }))
    }

    private fun handleAccessResponse(response: AccessResponse) {
        defaultSharedPreferences.apply(PREF_ACCESS_TOKEN to response.access_token,
                                       PREF_REFRESH_TOKEN to response.refresh_token)
        startActivity<SettingsActivity>()
    }

    private fun showError() {
        // TODO
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

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            ref.get()?.onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            ref.get()?.onPageFinished()
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private class MyWebViewClientNougat(activity: LoginActivity,
                                        val ref: WeakReference<LoginActivity> = WeakReference(activity)) : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            ref.get()?.handleUri(request.url) ?: false

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            ref.get()?.onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            ref.get()?.onPageFinished()
        }
    }
}

data class SubmitEvent(val code: String)

data class SubmitUiModel(val inProgress: Boolean = false,
                         val success: Boolean = false,
                         val response: AccessResponse? = null,
                         val error: AccessErrorResponse? = null) {

    companion object {
        fun inProgress() = SubmitUiModel(inProgress = true)
        fun success(response: AccessResponse) = SubmitUiModel(success = true, response = response)
        fun failure(error: AccessErrorResponse?) = SubmitUiModel(error = error)
    }
}
