package com.ysy.csdn;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ysy.csdn.adapter.NewContentAdapter;
import com.zhy.bean.CommonException;
import com.zhy.bean.News;
import com.zhy.biz.NewsItemBiz;

import java.util.List;

import me.maxwin.view.IXListViewLoadMore;
import me.maxwin.view.XListView;

/**
 * Created by ysy on 2015/4/10.
 */
public class NewsContentActivity extends Activity implements IXListViewLoadMore {
    public static final double DOUBLE = 68.;
    private XListView mListView;
    /**
     * ��ҳ���url
     */
    private String url;
    private NewsItemBiz mNewsItemBiz;
    private List<News> mDatas;

    private ProgressBar mProgressBar;
    private NewContentAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_content);

        mListView = (XListView) findViewById(R.id.id_listview);
        mProgressBar = (ProgressBar) findViewById(R.id.id_newsContentPro);

        mNewsItemBiz = new NewsItemBiz();
        Bundle extras = getIntent().getExtras();
        url = extras.getString("url");
        mAdapter = new NewContentAdapter(this);

        mListView.setAdapter(mAdapter);
        mListView.disablePullRefreash();
        mListView.setPullLoadEnable(this);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                News news = mDatas.get(position - 1);
                String imageLink = news.getImageLink();

                Intent intent = new Intent(NewsContentActivity.this, ImageShowActivity.class);
                intent.putExtra("url", imageLink);
                startActivity(intent);

            }
        });

        mProgressBar.setVisibility(View.VISIBLE);
        new LoadDataTask().execute();

    }

    @Override
    public void onLoadMore() {

    }

    private class LoadDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                mDatas = mNewsItemBiz.getNews(url).getNewses();
            } catch (CommonException e) {
                Looper.prepare();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

            return null;

        }

        @Override
        protected void onPostExecute(Void result) {
            if (mDatas == null)
                return;
            mAdapter.addList(mDatas);
            mAdapter.notifyDataSetChanged();
            mProgressBar.setVisibility(View.GONE);
        }

    }

    /**
     * 118.     * ������ذ�ť
     * 119.     * @param view
     * 120.
     */
    public void back(View view) {
        finish();
    }

}
