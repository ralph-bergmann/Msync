package eu.the4thfloor.msync.ui

import android.Manifest
import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.annotation.RequiresPermission
import android.view.View
import android.view.Window
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.firebase.crash.FirebaseCrash
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException
import com.squareup.moshi.Moshi
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_KEY
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_REDIRECT_URI
import eu.the4thfloor.msync.BuildConfig.MEETUP_OAUTH_SECRET
import eu.the4thfloor.msync.MSyncApp
import eu.the4thfloor.msync.R
import eu.the4thfloor.msync.api.MeetupApi
import eu.the4thfloor.msync.api.SecureApi
import eu.the4thfloor.msync.api.models.AccessResponse
import eu.the4thfloor.msync.api.models.CreateAccountResponse
import eu.the4thfloor.msync.api.models.ErrorResponse
import eu.the4thfloor.msync.api.models.Response
import eu.the4thfloor.msync.api.models.SelfResponse
import eu.the4thfloor.msync.utils.checkSelfPermission
import eu.the4thfloor.msync.utils.createAccount
import eu.the4thfloor.msync.utils.createCalendar
import eu.the4thfloor.msync.utils.doFromSdk
import eu.the4thfloor.msync.utils.getCalendarID
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class LoginActivity : AccountAuthenticatorActivity() {

    @Inject lateinit var secureApi: SecureApi
    @Inject lateinit var meetupApi: MeetupApi
    @Inject lateinit var moshi: Moshi

    private val disposables = CompositeDisposable()
    private val request = PublishProcessor.create<Request>()

    private var refreshToken: String? = null
    private var account: Account? = null

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
                .build().toString()

            Timber.i("request: %s", url)
            it.loadUrl(url)
        }
    }

    override fun onStart() {
        super.onStart()

        val accessTransformer =
            { flowable: Flowable<Request.Access> ->
                flowable
                    .flatMap { access ->
                        secureApi.access(mapOf("client_id" to MEETUP_OAUTH_KEY,
                                               "client_secret" to MEETUP_OAUTH_SECRET,
                                               "redirect_uri" to MEETUP_OAUTH_REDIRECT_URI,
                                               "code" to access.code,
                                               "grant_type" to "authorization_code"))
                            .map { response -> ResponseModel.success(response) }
                            .onErrorReturn { t ->
                                ResponseModel.failure(if (t is HttpException) {
                                    moshi
                                        .adapter(ErrorResponse::class.java)
                                        .fromJson(t.response().errorBody().string())
                                } else {
                                    null
                                })
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(ResponseModel.inProgress())
                    }
            }

        val selfTransformer =
            { flowable: Flowable<Request.Self> ->
                flowable
                    .flatMap { self ->
                        meetupApi.self("Bearer ${self.accessToken}")
                            .map { response -> ResponseModel.success(response) }
                            .onErrorReturn { t ->
                                ResponseModel.failure(if (t is HttpException) {
                                    moshi
                                        .adapter(ErrorResponse::class.java)
                                        .fromJson(t.response().errorBody().string())
                                } else {
                                    null
                                })
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(ResponseModel.inProgress())
                    }
            }

        val createAccountTransformer =
            { flowable: Flowable<Request.CreateAccount> ->
                flowable
                    .map { createAccount ->
                        val account = createAccount(createAccount.name, createAccount.refreshToken)
                        ResponseModel.success(CreateAccountResponse(account))
                    }
                    .startWith(ResponseModel.inProgress())
            }

        val requestTransformer = FlowableTransformer<Request, ResponseModel<Response>> { flowable ->
            flowable
                .publish({ shared ->
                             Flowable.merge(
                                 shared.ofType(Request.Access::class.java).compose(accessTransformer),
                                 shared.ofType(Request.Self::class.java).compose(selfTransformer),
                                 shared.ofType(Request.CreateAccount::class.java).compose(createAccountTransformer))
                         })
        }

        disposables
            .add(request
                     .compose(requestTransformer)
                     .subscribe({ (inProgress, success, response, error) ->
                                    progressBar.visibility = if (inProgress) View.VISIBLE else View.GONE
                                    if (!inProgress) {
                                        if (success && response != null) {
                                            when (response) {
                                                is AccessResponse -> {
                                                    Timber.i("success %s", response.access_token)
                                                    handleResponse(response)
                                                }
                                                is SelfResponse -> {
                                                    Timber.i("self %s", response)
                                                    handleResponse(response)
                                                }
                                                is CreateAccountResponse -> {
                                                    Timber.i("account %s", response)
                                                    handleResponse(response)
                                                }
                                            }
                                        } else {
                                            Timber.e("!success %s", error)
                                            FirebaseCrash.report(Exception(error?.error_description))
                                            showError()
                                        }
                                    }
                                },
                                { error ->
                                    Timber.e(error, "failed to access api")
                                    progressBar.visibility = View.GONE
                                    FirebaseCrash.report(error)
                                    showError()
                                }))
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
        request.onNext(Request.Access(code))
    }

    private fun handleResponse(response: AccessResponse) {
        refreshToken = response.refresh_token
        request.onNext(Request.Self(response.access_token!!))
    }

    private fun handleResponse(response: SelfResponse) {
        request.onNext(Request.CreateAccount(refreshToken!!, response.name!!))
    }

    private fun handleResponse(response: CreateAccountResponse) {
        account = response.account
        doFromSdk(Build.VERSION_CODES.M,
                  {
                      checkCalendarPermissions()
                  },
                  {
                      createCalendar()
                  })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkCalendarPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR,
                                       Manifest.permission.WRITE_CALENDAR), 0)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {
                if (grantResults.size == 2
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    createCalendar()
                }
            }
        }
    }

    @RequiresPermission(allOf = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
    private fun createCalendar() {
        if (getCalendarID(account!!) == null) {
            createCalendar(account!!)
        }
        goNext()
    }

    private fun goNext() {
        startActivity<SettingsActivity>()
    }

    private fun showError() {
        // TODO
    }

    private class MyWebViewClient(activity: LoginActivity,
                                  private val ref: WeakReference<LoginActivity> = WeakReference(activity)) : WebViewClient() {


        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            ref.get()?.handleUri(Uri.parse(url)) ?: false

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            ref.get()?.onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            ref.get()?.onPageFinished()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private class MyWebViewClientNougat(activity: LoginActivity,
                                        private val ref: WeakReference<LoginActivity> = WeakReference(activity)) : WebViewClient() {

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

sealed class Request {
    class Access(val code: String) : Request()
    class Self(val accessToken: String) : Request()
    class CreateAccount(val refreshToken: String, val name: String) : Request()
    class Calendar(val accessToken: String) : Request()
}

data class ResponseModel<out T>(val inProgress: Boolean = false,
                                val success: Boolean = false,
                                val response: T? = null,
                                val error: ErrorResponse? = null) {

    companion object {
        fun <T> inProgress() = ResponseModel<T>(inProgress = true)
        fun <T> success(response: T) = ResponseModel(success = true, response = response)
        fun <T> failure(error: ErrorResponse?) = ResponseModel<T>(error = error)
    }
}
