package com.teneasy.chatuisdk.ui.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.android.common.adapter.ChatAdapter;
//import com.blankj.utilcode.util.BarUtils;
import com.effective.android.panel.PanelSwitchHelper;

public class AutoHidePanelRecyclerView extends RecyclerView {

    PanelSwitchHelper panelSwitchHelper;
   // private MessageLoadHandler mLoadHandler;
    private boolean mHasMoreForwardMessages=false;

    private boolean mHasMoreNewerMessages=false;

    public void setPanelSwitchHelper(PanelSwitchHelper panelSwitchHelper) {
        this.panelSwitchHelper = panelSwitchHelper;
    }

    public AutoHidePanelRecyclerView(Context context) {
        this(context, null);
    }

    public AutoHidePanelRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public AutoHidePanelRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      /*  if (e != null && e.getAction() != MotionEvent.ACTION_CANCEL) {
            if (panelSwitchHelper != null) {
                panelSwitchHelper.hookSystemBackByPanelSwitcher();
            }
        }*/
        return super.onTouchEvent(e);
    }

//  public void setLoadHandler(MessageLoadHandler loadHandler) {
//        mLoadHandler = loadHandler;
//    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
//        if (Math.abs(oldh - h)
//                > BarUtils.getStatusBarHeight() + BarUtils.getNavBarHeight()) {
//            if (mHasMoreNewerMessages) {
//                scrollBy(0, oldh - h);
//            } else {
//                scrollToEnd();
//            }
//        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void scrollToEnd() {
        if (getAdapter() != null) {
            int itemCount = getAdapter().getItemCount();
            if (itemCount > 0) {
                post(() -> scrollToPosition(itemCount - 1));
            }
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
//            if (mLoadHandler != null && getLayoutManager() != null) {
//                LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
//                int firstPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
//                int lastPosition = layoutManager.findLastCompletelyVisibleItemPosition();
//                if (firstPosition == 0 && mHasMoreForwardMessages) {
//                   // ChatAdapter adapter = (ChatAdapter) getAdapter();
//                 //   mLoadHandler.loadMoreForward(adapter.getFirstMessage().getMessage());
//                } /*else if (isLastItemVisibleCompleted()) {
//                    // mLoadHandler.loadMoreBackground(messageAdapter.getlastMessage());
//                }*/
//            }

        }
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);

    }

    public void setHasMoreForwardMessages(boolean hasMore) {
        mHasMoreForwardMessages = hasMore;
    }

    public void setHasMoreNewerMessages(boolean hasMore) {
        mHasMoreNewerMessages = hasMore;
    }

    /*private boolean isLastItemVisibleCompleted() {
    }*/
}
