package jasonngor.com.redditsavesearch.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;

import jasonngor.com.redditsavesearch.R;

public class MainActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ProgressBar spinner;
    private UserContributionPaginator paginator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        spinner = (ProgressBar) findViewById(R.id.progressBar2);
        spinner.setVisibility(View.VISIBLE);

        RedditClient reddit = AuthenticationManager.get().getRedditClient();
        String loggedUser = reddit.getAuthenticatedUser();
        paginator = new UserContributionPaginator(reddit, "saved", loggedUser);
        paginator.setLimit(1000);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        getAllSavedAsyncTask();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void getAllSavedAsyncTask() {
        new AsyncTask<UserContributionPaginator, Void, ArrayList<Contribution>>() {
            private ArrayList<Contribution> savedList;
            @Override
            protected ArrayList<Contribution> doInBackground(UserContributionPaginator... params) {
                savedList = new ArrayList<>();
                while (paginator.hasNext()) {
                    for (Contribution c:params[0].next()) {
                        savedList.add(c);
                    }
                }
                adapter = new MyAdapter(savedList);
                return savedList;
            }

            @Override
            protected void onPostExecute(ArrayList<Contribution> savedList) {
                spinner.setVisibility(View.GONE);
                recyclerView.setAdapter(adapter);
                Log.d("setadapter", String.valueOf(true));
            }
        }.execute(paginator);
    }
    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ContributionViewHolder> {
        private ArrayList<Contribution> savedList;

        public class ContributionViewHolder extends RecyclerView.ViewHolder {
            public TextView vContributionContent;
            public TextView vContributionNum;

            public ContributionViewHolder(View v) {
                super(v);
                vContributionNum = (TextView) v.findViewById(R.id.txtContributionNum);
                vContributionContent = (TextView) v.findViewById(R.id.txtContributionContent);
            }
        }

        public MyAdapter(ArrayList<Contribution> dataset) {
            this.savedList = dataset;
        }

        @Override
        public ContributionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contribution_card, parent, false);
            return new ContributionViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ContributionViewHolder holder, int position) {
            final Contribution contribution = savedList.get(position);
            holder.vContributionContent.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Uri address;
                    if (contribution instanceof Submission) {
                        address = Uri.parse(((Submission) contribution).getUrl());
                    } else {
                        address = Uri.parse(((Comment) contribution).getUrl());
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW, address);
                    startActivity(intent);
                }
            });

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
    }
}
