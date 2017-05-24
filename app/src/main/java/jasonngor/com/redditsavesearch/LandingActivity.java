package jasonngor.com.redditsavesearch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LandingActivity extends AppCompatActivity {
    private UserAgent myUserAgent;
    private RedditClient redditClient;
    private OAuthHelper oAuthHelper;
    private Credentials appCredentials;
    private WebView webView;
    private Context context;
    private RequestQueue requestQueue;

    private final String clientId = "SgPuJGeI5QBmiw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        context = this;

        myUserAgent = UserAgent.of("android", "jasonngor.com.redditsavesearch", "v0.1", "nibcakes");
        redditClient = new RedditClient(myUserAgent);
        oAuthHelper = redditClient.getOAuthHelper();
        appCredentials = Credentials.installedApp(clientId, "https://github.com/jasonngor/RedditSaveSearch");
        URL authorizationURL = oAuthHelper.getAuthorizationUrl(appCredentials, true, true,
                new String[]{"account", "history", "identity", "read", "save"});
        webView = (WebView) findViewById(R.id.wvLogin);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                final String redirectUrl = request.getUrl().toString();
                Log.d("URL redirect: ", redirectUrl);
                if (redirectUrl.startsWith("https://github.com/jasonngor/RedditSaveSearch")) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                OAuthData oAuthData = oAuthHelper.onUserChallenge(redirectUrl, appCredentials);
                                Log.d("oAuthData", oAuthData.toString());
                                Log.d("Authorization status", String.valueOf(oAuthHelper.getAuthStatus() == OAuthHelper.AuthStatus.AUTHORIZED));
                                redditClient.authenticate(oAuthData);
                                Log.d("Authenticated User", redditClient.getAuthenticatedUser());
                                Intent intent = new Intent(context, HomeActivity.class);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.d("OAuthData failed", e.toString());
                            }
                        }
                    });
                    thread.start();
                    return true;
                }
                return false;
            }
        });
        webView.loadUrl(authorizationURL.toString());
    }
}