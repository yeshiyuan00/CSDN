package com.ysy.csdn;

import android.annotation.SuppressLint;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;


import com.ysy.csdn.adapter.NewsItemAdapter;
import com.ysy.csdn.dao.NewsItemDao;
import com.ysy.csdn.util.AppUtil;
import com.ysy.csdn.util.Logger;
import com.ysy.csdn.util.NetUtil;
import com.ysy.csdn.util.ToastUtil;
import com.zhy.bean.CommonException;
import com.zhy.bean.NewsItem;
import com.zhy.biz.NewsItemBiz;
import com.zhy.csdn.Constaint;

import java.util.ArrayList;
import java.util.List;

import me.maxwin.view.IXListViewLoadMore;
import me.maxwin.view.IXListViewRefreshListener;
import me.maxwin.view.XListView;

/**
 * Created by ysy on 2015/4/9.
 */
@SuppressLint("ValidFragment")
public class MainFragment extends Fragment implements IXListViewRefreshListener, IXListViewLoadMore {
    private static final int LOAD_MORE = 0x110;
    private static final int LOAD_REFREASH = 0x111;

    private static final int TIP_ERROR_NO_NETWORK = 0X112;
    private static final int TIP_ERROR_SERVER = 0X113;
    /**
     * 是否是第一次进入
     */
    private boolean isFirstIn = true;

    /**
     * 是否连接网络
     */
    private boolean isConnNet = false;

    /**
     * 当前数据是否是从网络中获取的
     */
    private boolean isLoadingDataFromNetWork;
    /**
     * 与数据库交互
     */
    private NewsItemDao mNewsItemDao;

    /**
     * 默认的newType
     */
    private int newsType = Constaint.NEWS_TYPE_YEJIE;
    /**
     * 当前页面
     */
    private int currentPage = 0;

    /**
     * 处理新闻的业务类
     */
    private NewsItemBiz mNewsItemBiz;
    /*
    * 扩展的ListView
    * */
    private XListView mXListView;
    /*
    * 数据适配器
    * */
    private NewsItemAdapter mAdapter;
    /*
    * 数据
    * */
    private List<NewsItem> mDatas = new ArrayList<NewsItem>();

    /**
     * 获得newsType
     *
     * @param newsType
     */
    public MainFragment(int newsType) {
        this.newsType = newsType;
        Logger.e(newsType + "newsType");
        mNewsItemBiz = new NewsItemBiz();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tab_item_fragment_main, null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mNewsItemDao = new NewsItemDao(getActivity());
        mAdapter = new NewsItemAdapter(getActivity(), mDatas);
        /**
         * 初始化
         * */
        mXListView = (XListView) getView().findViewById(R.id.id_xlistView);
        mXListView.setAdapter(mAdapter);
        mXListView.setPullRefreshEnable(this);
        mXListView.setPullLoadEnable(this);
        mXListView.setRefreshTime(AppUtil.getRefreashTime(getActivity(), newsType));

        mXListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewsItem newsItem=mDatas.get(position-1);
                Intent intent=new Intent(getActivity(), NewsContentActivity.class);
                intent.putExtra("url", newsItem.getLink());
                startActivity(intent);
            }
        });

        if (isFirstIn) {
            /**
             * 进来时直接刷新
             */
            mXListView.startRefresh();
            isFirstIn = false;
        } else {
            mXListView.NotRefreshAtBegin();
        }


    }

    @Override
    public void onRefresh() {
        new LoadDataTask().execute(LOAD_REFREASH);
    }

    @Override
    public void onLoadMore() {
        new LoadDataTask().execute(LOAD_MORE);
    }

    /**
     * 记载数据的异步任务
     *
     * @author ysy
     */
    class LoadDataTask extends AsyncTask<Integer, Void, Integer> {


        @Override
        protected Integer doInBackground(Integer... params) {
            switch (params[0]) {
                case LOAD_MORE:
                    loadMoreData();
                    break;
                case LOAD_REFREASH:
                    return refreashData();
            }
            return -1;

        }

        @Override
        protected void onPostExecute(Integer result) {

            switch (result) {
                case TIP_ERROR_NO_NETWORK:
                    ToastUtil.toast(getActivity(), "没有网络连接");
                    mAdapter.setDatas(mDatas);
                    mAdapter.notifyDataSetChanged();
                    break;
                case TIP_ERROR_SERVER:
                    ToastUtil.toast(getActivity(), "服务器错误！");
                    break;
                default:
                    break;
            }
            mXListView.setRefreshTime(AppUtil.getRefreashTime(getActivity(), newsType));
            mXListView.stopRefresh();
            mXListView.stopLoadMore();
        }
    }

    /**
     * 会根据当前网络情况，判断是从数据库加载还是从网络继续获取
     */
    private void loadMoreData() {

        //当前数据是从网络上获取的
        if (isLoadingDataFromNetWork) {
            currentPage += 1;

            try {
                List<NewsItem> newsItems = mNewsItemBiz.getNewsItems(newsType, currentPage);
                mNewsItemDao.add(newsItems);
                mAdapter.addAll(newsItems);
            } catch (CommonException e) {
                e.printStackTrace();
            }
        } else {
            //从数据库加载
            currentPage += 1;
            List<NewsItem> newsItems = mNewsItemDao.list(newsType, currentPage);
            mAdapter.addAll(newsItems);
        }

    }

    /**
     * 下拉刷新数据
     */
    public Integer refreashData() {
        if (NetUtil.checkNet(getActivity())) {
            isConnNet = true;
            //获取最新数据
            try {
                List<NewsItem> newsItems = mNewsItemBiz.getNewsItems(newsType, currentPage);
                mAdapter.setDatas(newsItems);
                isLoadingDataFromNetWork = true;
                //设置刷新时间
                AppUtil.setRefreashTime(getActivity(), newsType);
                //清楚数据库数据
                mNewsItemDao.deleteAll(newsType);
                //存入数据库数据
                mNewsItemDao.add(newsItems);
            } catch (CommonException e) {
                e.printStackTrace();
                isLoadingDataFromNetWork = false;
                return TIP_ERROR_SERVER;
            }
        } else {
            isConnNet = false;
            isLoadingDataFromNetWork = false;
            // TODO从数据库中加载
            List<NewsItem> newsItems = mNewsItemDao.list(newsType, currentPage);
            mDatas = newsItems;
            //mAdapter.setDatas(newsItems);
            return TIP_ERROR_NO_NETWORK;
        }
        return -1;
    }


}
