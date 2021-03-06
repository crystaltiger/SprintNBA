package com.yuyh.sprintnba.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.yuyh.library.utils.DimenUtils;
import com.yuyh.library.utils.log.LogUtils;
import com.yuyh.library.view.viewpager.indicator.IndicatorViewPager;
import com.yuyh.library.view.viewpager.indicator.ScrollIndicatorView;
import com.yuyh.sprintnba.R;
import com.yuyh.sprintnba.base.BaseSwipeBackCompatActivity;
import com.yuyh.sprintnba.event.RefreshCompleteEvent;
import com.yuyh.sprintnba.event.RefreshEvent;
import com.yuyh.sprintnba.http.bean.match.MatchBaseInfo;
import com.yuyh.sprintnba.ui.adapter.VPGameDetailAdapter;
import com.yuyh.sprintnba.ui.presenter.impl.MatchDetailPresenter;
import com.yuyh.sprintnba.ui.view.MatchDetailView;
import com.yuyh.sprintnba.utils.FrescoUtils;
import com.yuyh.sprintnba.utils.StatusBarCompat;
import com.yuyh.sprintnba.widget.GameDetailScrollBar;
import com.yuyh.sprintnba.widget.StickyNavLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.InjectView;

/**
 * @author yuyh.
 * @date 16/6/11.
 */
public class MatchDetailActivity extends BaseSwipeBackCompatActivity implements MatchDetailView, StickyNavLayout.OnStickStateChangeListener {

    public static final String INTENT_MID = "mid";
    public static final String INTENT_YEAR = "year";
    private String mid;
    private String year;

    public static void start(Context context, String mid, String year) {
        Intent intent = new Intent(context, MatchDetailActivity.class);
        intent.putExtra(MatchDetailActivity.INTENT_MID, mid);
        intent.putExtra(MatchDetailActivity.INTENT_YEAR, year);
        context.startActivity(intent);
    }

    @InjectView(R.id.snlViewPager)
    ViewPager viewPager;
    @InjectView(R.id.snlIindicator)
    ScrollIndicatorView indicator;
    @InjectView(R.id.stickyNavLayout)
    StickyNavLayout stickyNavLayout;

    @InjectView(R.id.rlMatchToolbar)
    RelativeLayout rlMatchToolbar;
    @InjectView(R.id.tvBack)
    TextView tvBack;
    @InjectView(R.id.tvMatchTitle)
    TextView tvMatchTitle;
    @InjectView(R.id.tvLeftRate)
    TextView tvLeftRate;
    @InjectView(R.id.tvMatchState)
    TextView tvMatchState;
    @InjectView(R.id.tvRightRate)
    TextView tvRightRate;
    @InjectView(R.id.tvMatchLeftScore)
    TextView tvMatchLeftScore;
    @InjectView(R.id.tvMatchType)
    TextView tvMatchType;
    @InjectView(R.id.tvMatchRightScore)
    TextView tvMatchRightScore;
    @InjectView(R.id.tvMatchStartTime)
    TextView tvMatchStartTime;

    @InjectView(R.id.ivMatchLeftTeam)
    SimpleDraweeView ivMatchLeftTeam;
    @InjectView(R.id.ivMatchRightTeam)
    SimpleDraweeView ivMatchRightTeam;

    @InjectView(R.id.swipeLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    private IndicatorViewPager indicatorViewPager;
    private VPGameDetailAdapter adapter;
    private MatchDetailPresenter presenter;
    private boolean isNeedUpdateTab = true;

    private MatchBaseInfo.BaseInfo info;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_game_detail;
    }

    @Override
    protected void initViewsAndEvents() {
        EventBus.getDefault().register(this);
        mid = getIntent().getStringExtra(INTENT_MID);
        year = getIntent().getStringExtra(INTENT_YEAR);
        LogUtils.i(year);

        //rlMatchToolbar.getBackground().setAlpha(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rlMatchToolbar.getLayoutParams().height += StatusBarCompat.getStatusBarHeight(this);
            rlMatchToolbar.setPadding(0, StatusBarCompat.getStatusBarHeight(this), 0, 0);
        }

        indicator.setScrollBar(new GameDetailScrollBar(getApplicationContext(), ContextCompat.getColor(this, R.color.colorPrimary), DimenUtils.dpToPxInt(3)));
        indicatorViewPager = new IndicatorViewPager(indicator, viewPager);
        stickyNavLayout.setOnStickStateChangeListener(this);

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.material_red, R.color.material_green);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

        presenter = new MatchDetailPresenter(this, this);
        presenter.getMatchBaseInfo(mid);
    }

    @Override
    public void showTabViewPager(String[] names, boolean isStart) {
        isNeedUpdateTab = false;
        hideLoadingDialog();
        adapter = new VPGameDetailAdapter(this, names, getSupportFragmentManager(), mid, isStart, info);
        indicatorViewPager.setAdapter(adapter);
    }

    @Override
    public void showMatchInfo(MatchBaseInfo.BaseInfo info) {
        this.info = info;

        tvMatchTitle.setText(info.leftName + "vs" + info.rightName);
        if (!TextUtils.isEmpty(info.leftWins) && !TextUtils.isEmpty(info.leftLosses))
            tvLeftRate.setText(info.leftWins + "胜" + info.leftLosses + "负");
        if (!TextUtils.isEmpty(info.rightWins) && !TextUtils.isEmpty(info.rightLosses))
            tvRightRate.setText(info.rightWins + "胜" + info.rightLosses + "负");

        String startTime = info.startDate + info.startHour;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM月dd日HH:mm");
        // 当前比赛状态
        String state = "未开始";
        try {
            Date startDate = format.parse(year + startTime);
            Date todayDate = new Date();
            if (startDate.getTime() > todayDate.getTime()) { // 未开始
                if (isNeedUpdateTab) { // 更新TAB
                    presenter.getTab(false);
                }
            } else {
                state = info.quarterDesc;
                // 第四节 00:00 或 加时n 00:00 表示比赛已经结束
                if (((state.contains("第4节") || state.contains("加时"))
                        && !info.leftGoal.equals(info.rightGoal))
                        && state.contains("00:00")) {
                    state = "已结束";
                }
                if (isNeedUpdateTab) {
                    presenter.getTab(true);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        tvMatchState.setText(state);
        tvMatchType.setText(info.desc);
        tvMatchStartTime.setText(info.startDate + "   " + info.startHour + "   " + info.venue);
        tvMatchLeftScore.setText(info.leftGoal);
        tvMatchRightScore.setText(info.rightGoal);
        ivMatchLeftTeam.setController(FrescoUtils.getController(info.leftBadge, ivMatchLeftTeam));
        ivMatchRightTeam.setController(FrescoUtils.getController(info.rightBadge, ivMatchRightTeam));
    }

    private boolean lastIsTopHidden;//记录上次是否悬浮

    @Override
    public void isStick(boolean isStick) {
        if (lastIsTopHidden != isStick) {
            lastIsTopHidden = isStick;
        }
    }

    @Override
    public void scrollPercent(float percent) {
        int color = Color.argb((int) ((float) 255 * percent), 26, 142, 168);
        rlMatchToolbar.setBackgroundColor(color);
        if (percent == 0) {
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        } else {
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setOnRefreshListener(null);
        }
    }

    @Override
    public void showLoading(String msg) {
        showLoadingDialog();
    }

    @Override
    public void hideLoading() {
        hideLoadingDialog();
    }

    @Override
    public void showError(String msg) {

    }

    @Subscribe
    public void onEventMainThread(RefreshCompleteEvent event) {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            presenter.getMatchBaseInfo(mid);
            EventBus.getDefault().post(new RefreshEvent());
        }
    };

}
