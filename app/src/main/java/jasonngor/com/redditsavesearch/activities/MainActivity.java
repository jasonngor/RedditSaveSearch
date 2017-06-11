package jasonngor.com.redditsavesearch.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        spinner = (ProgressBar) findViewById(R.id.progressBar2);

        reddit = AuthenticationManager.get().getRedditClient();
        loggedUser = reddit.getAuthenticatedUser();

        recyclerView = (FastScrollRecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        getAllSavedAsyncTask();
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
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            if (item != exception) item.setVisible(visible);
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ContributionViewHolder> {
        private ArrayList<Contribution> savedList;
        private ArrayList<Contribution> savedListCopy;
        private int expandedPosition = -1;

        public class ContributionViewHolder extends RecyclerView.ViewHolder {
            private TextView vContributionContent;
            private TextView vContributionNum;
            private RelativeLayout unexpandedLayout;
            private RelativeLayout expandedLayout;
            private ImageButton btnComments;
            private ImageButton btnSave;
            private ImageButton btnLink;

            private ContributionViewHolder(View v) {
                super(v);
                vContributionNum = (TextView) v.findViewById(R.id.txtContributionNum);
                vContributionContent = (TextView) v.findViewById(R.id.txtContributionContent);
                unexpandedLayout = (RelativeLayout) v.findViewById(R.id.unexpandedLayout);
                expandedLayout = (RelativeLayout) v.findViewById(R.id.expandedLayout);
                btnComments = (ImageButton) v.findViewById(R.id.btnComments);
                btnSave = (ImageButton) v.findViewById(R.id.btnSave);
                btnLink = (ImageButton) v.findViewById(R.id.btnLink);
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
        public void onBindViewHolder(ContributionViewHolder holder, final int position) {
            final Contribution contribution = savedList.get(position);
            final boolean isExpanded = position == expandedPosition;
            holder.expandedLayout.setVisibility(isExpanded ? View.VISIBLE:View.GONE);

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

            //open url
//            @Override public void onClick(View v) {
//                Uri address;
//                if (contribution instanceof Submission) {
//                    address = Uri.parse(((Submission) contribution).getUrl());
//                } else {
//                    address = Uri.parse(((Comment) contribution).getUrl());
//                }
//                Intent intent = new Intent(Intent.ACTION_VIEW, address);
//                startActivity(intent);
//            }
            holder.vContributionNum.setText(String.format("#%d", position + 1));
            if (contribution instanceof Submission){
                holder.vContributionContent.setText(((Submission)contribution).getTitle());
            } else if (contribution instanceof Comment) {
                holder.vContributionContent.setText(((Comment)contribution).getSubmissionTitle());
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
                for (Contribution c: savedListCopy) {
                    if (c instanceof Submission) {
                        if (((Submission) c).getTitle().toLowerCase().contains(text)) {
                            savedList.add(c);
                        }
                    } else {
                        if (((Comment) c).getSubmissionTitle().toLowerCase().contains(text)) {
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
    }
}
