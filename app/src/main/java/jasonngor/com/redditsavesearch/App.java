package jasonngor.com.redditsavesearch;

import android.app.Application;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.auth.VolatileTokenStore;

/**
 * Created by Jason Ngor on 5/26/2017.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RedditClient reddit = new AndroidRedditClient(this);
        RefreshTokenHandler handler = new RefreshTokenHandler(new VolatileTokenStore(), reddit);
        AuthenticationManager.get().init(reddit, handler);
    }
}
