package jasonngor.com.redditsavesearch.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import jasonngor.com.redditsavesearch.R;

public class MainActivity extends BaseActivity {
    private FastScrollRecyclerView recyclerView;
    private RecyclerAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ProgressBar spinner;
    private UserContributionPaginator paginator;
    private RedditClient reddit;
    private String loggedUser;
    private ArrayList<Contribution> savedList;
    private AccountManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        spinner = (ProgressBar) findViewById(R.id.progressBar2);

        try {
            reddit = AuthenticationManager.get().getRedditClient();
            loggedUser = reddit.getAuthenticatedUser();
            manager = new AccountManager(reddit);

            recyclerView = (FastScrollRecyclerView) findViewById(R.id.recyclerView);
            recyclerView.setHasFixedSize(true);
            layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            getAllSavedAsyncTask();
        } catch (IllegalStateException | NetworkException e) {
            MainActivity.this.finish();
        }
    }

    public void getAllSavedAsyncTask() {
        paginator = new UserContributionPaginator(reddit, "saved", loggedUser);
        paginator.setLimit(1000);

        new AsyncTask<UserContributionPaginator, Void, ArrayList<Contribution>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (adapter != null) {
                    adapter.clear();
                }
                spinner.setVisibility(View.VISIBLE);
            }

            @Override
            protected ArrayList<Contribution> doInBackground(UserContributionPaginator... params) {

                savedList = new ArrayList<>();
                while (paginator.hasNext()) {
                    for (Contribution c:params[0].next()) {
                        savedList.add(c);
                    }
                }
                adapter = new RecyclerAdapter(savedList);
                return savedList;
            }

            @Override
            protected void onPostExecute(ArrayList<Contribution> savedList) {
                spinner.setVisibility(View.GONE);
                recyclerView.setAdapter(adapter);
            }
        }.execute(paginator);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.search_toolbar_item);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setItemsVisibility(menu, searchItem, false);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                setItemsVisibility(menu, searchItem, true);
                MainActivity.this.invalidateOptionsMenu();
                return false;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
        return true;
    }

    private void setItemsVisibility(Menu menu, MenuItem exception, boolean visible) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item != exception) item.setVisible(visible);
            Log.d("itemsetvisible", item.toString() + String.valueOf(item.isVisible()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout_toolbar_item:
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        AuthenticationManager.get().getRedditClient().getOAuthHelper().revokeAccessToken(LoginActivity.CREDENTIALS);
                        AuthenticationManager.get().getRedditClient().deauthenticate();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        MainActivity.this.finish();
                    }
                }.execute();

                return true;

            case R.id.refresh_toolbar_item:
                getAllSavedAsyncTask();
                return true;

            case R.id.random_toolbar_item:
                adapter.random();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ContributionViewHolder> {
        private ArrayList<Contribution> savedList;
        private ArrayList<Contribution> savedListCopy;
        private int expandedPosition = -1;
        private boolean random = false;

        public class ContributionViewHolder extends RecyclerView.ViewHolder {
            private TextView vContributionContent;
            private TextView vContributionNum;
            private TextView vContributionType;
            private TextView vAdditionalInfo;
            private RelativeLayout unexpandedLayout;
            private RelativeLayout expandedLayout;
            private ImageButton btnLink;
            private ImageButton btnComments;
            private ImageButton btnSave;

            private ContributionViewHolder(View v) {
                super(v);
                vContributionNum = (TextView) v.findViewById(R.id.txtContributionNum);
                vContributionContent = (TextView) v.findViewById(R.id.txtContributionContent);
                vContributionType = (TextView) v.findViewById(R.id.txtContributionType);
                vAdditionalInfo = (TextView) v.findViewById(R.id.txtAdditionalInfo);
                btnSave = (ImageButton) v.findViewById(R.id.btnSave);
                unexpandedLayout = (RelativeLayout) v.findViewById(R.id.unexpandedLayout);
                expandedLayout = (RelativeLayout) v.findViewById(R.id.expandedLayout);
                btnLink = (ImageButton) v.findViewById(R.id.btnLink);
                btnComments = (ImageButton) v.findViewById(R.id.btnComments);
            }
        }

        private RecyclerAdapter(ArrayList<Contribution> dataset) {
            this.savedList = dataset;
            this.savedListCopy = new ArrayList<>(dataset);
        }

        @Override
        public ContributionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contribution_card, parent, false);
            return new ContributionViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ContributionViewHolder holder, final int position) {
            final Contribution contribution = savedList.get(position);
            final boolean isExpanded = position == expandedPosition;

            holder.expandedLayout.setVisibility(isExpanded ? View.VISIBLE:View.GONE);
            holder.vAdditionalInfo.setVisibility(isExpanded ? View.VISIBLE:View.GONE);

            holder.unexpandedLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (expandedPosition >= 0) {
                        int prev = expandedPosition;
                        notifyItemChanged(prev);
                    }
                    expandedPosition = isExpanded ? -1:position;
                    notifyItemChanged(position);
                }
            });

            holder.btnLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri address;
                    if (contribution instanceof Submission) {
                        address = Uri.parse(((Submission) contribution).getUrl());
                    } else {
                        address = Uri.parse("https://www.reddit.com/r/" +
                                ((Comment) contribution).getSubredditName() +
                                "/comments/" +
                                ((Comment) contribution).getSubmissionId().substring(3) +
                                "/" + ((Comment) contribution).getSubmissionTitle()
                                .replace(" ", "_")
                                .replaceAll("\\W", "") +
                                "/" + contribution.getId());
                    }
                    Log.d("clickaddress", address.toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, address);
                    startActivity(intent);
                }
            });

            holder.btnComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri address;
                    if (contribution instanceof Submission) {
                        address = Uri.parse("https://www.reddit.com" + ((Submission) contribution).getPermalink());
                    } else {
                        address = Uri.parse("https://www.reddit.com/r/" +
                                ((Comment) contribution).getSubredditName() +
                                "/comments/" +
                                ((Comment) contribution).getSubmissionId().substring(3));
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, address);
                    startActivity(intent);
                }
            });

            holder.btnSave.setOnClickListener(new View.OnClickListener() {
                boolean isSaved = true;
                @Override
                public void onClick(View v) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                if (isSaved) {
                                    if (contribution instanceof Submission) {
                                        manager.unsave((Submission) contribution);
                                    } else {
                                        manager.unsave((Comment) contribution);
                                    }
                                    isSaved = false;
                                } else {
                                    if (contribution instanceof Submission) {
                                        manager.save((Submission) contribution);
                                    } else {
                                        manager.save((Comment) contribution);
                                    }
                                    isSaved = true;
                                }

                            } catch (NetworkException | ApiException e) {
                                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            if (!isSaved) {
                                holder.btnSave.setImageResource(R.drawable.ic_star_border_black_24dp);
                                Toast.makeText(MainActivity.this, "Unsaved", Toast.LENGTH_SHORT).show();
                            } else {
                                holder.btnSave.setImageResource(R.drawable.ic_star_black_24dp);
                                Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }.execute();

                }
            });

            holder.vContributionNum.setText(String.format("#%d", position + 1));
            if (contribution instanceof Submission){
                holder.vContributionContent.setText(((Submission)contribution).getTitle());
                if (((Submission) contribution).isSelfPost()) {
                    holder.vContributionType.setText("Self Post");
                    holder.vAdditionalInfo.setText(((Submission) contribution).getSelftext());
                } else {
                    holder.vContributionType.setText("Submission");
                    holder.vAdditionalInfo.setText(((Submission) contribution).getUrl());
                }
            } else if (contribution instanceof Comment) {
                holder.vContributionContent.setText(((Comment)contribution).getSubmissionTitle());
                holder.vContributionType.setText("Comment");
                holder.vAdditionalInfo.setText(((Comment) contribution).getBody());
            }

        }

        @Override
        public int getItemCount() {
            return savedList.size();
        }

        public void filter(String text) {
            savedList.clear();
            if (text.isEmpty()) {
                savedList.addAll(savedListCopy);
            } else {
                text = text.toLowerCase();
                for (Contribution c : savedListCopy) {
                    if (c instanceof Submission) {
                        if (((Submission) c).getTitle().toLowerCase().contains(text) ||
                                (((Submission) c).getUrl().contains(text)) ||
                                (((Submission) c).getSelftext().contains(text))) {
                            savedList.add(c);
                        }
                    } else {
                        if (((Comment) c).getSubmissionTitle().toLowerCase().contains(text) ||
                                ((Comment) c).getBody().contains(text)) {
                            savedList.add(c);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        public void clear() {
            savedList.clear();
            notifyDataSetChanged();
        }

        public void random() {
            if (!random) {
                Random rand = new Random();
                savedList.clear();
                HashSet<Integer> added = new HashSet<>();
                while (added.size() < 10) {
                    added.add(rand.nextInt(savedListCopy.size()));
                }
                Iterator<Integer> addedIter = added.iterator();
                while (addedIter.hasNext()) {
                    savedList.add(savedListCopy.get(addedIter.next()));
                }
                random = true;
                notifyDataSetChanged();
            } else {
                savedList.clear();
                savedList.addAll(savedListCopy);
                random = false;
                notifyDataSetChanged();
            }
        }
    }
}
