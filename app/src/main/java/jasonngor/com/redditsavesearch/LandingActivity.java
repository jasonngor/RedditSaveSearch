package jasonngor.com.redditsavesearch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.UserContributionPaginator;

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
    private ProgressBar spinner;

    private final String clientId = "SgPuJGeI5QBmiw";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        context = this;
    }

    @Override
    protected void onResume() {
        super.onResume();

        spinner = (ProgressBar) findViewById(R.id.progressBar);
        AuthenticationState authState = AuthenticationManager.get().checkAuthState();
        Intent intent;

        switch (authState) {
            case READY:
                spinner.setVisibility(View.GONE);
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;

            case NONE:
                Toast.makeText(LandingActivity.this, "Log in first", Toast.LENGTH_SHORT).show();
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;

            case NEED_REFRESH:
                refreshAccessTokenAsync();
                break;
        }
    }

    private void refreshAccessTokenAsync() {
        new AsyncTask<Credentials, Void, Void>() {
            @Override
            protected Void doInBackground(Credentials... params) {
                try {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                    Intent intent = new Intent(LandingActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (NoSuchTokenException | OAuthException e){
                    Log.e("refreshAccessTokenAsync", "Failed to refresh access token", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                Log.d("refreshAccessTokenAsync", "Reauthenticated");
                spinner.setVisibility(View.GONE);
            }
        }.execute();
    }
}