package com.teneasy.chatuisdk.ui.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.teneasy.chatuisdk.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义底部菜单
 */
public class DialogBottomMenu {
    private Context context;

    private PopupWindow popupWindow;
    private List<String> items;

    private TextView tvTitle;
    private View contentView;
    private ListView listView;
    private BaseAdapter mAdapter;
    private AdapterView.OnItemClickListener onItemClickListener;

    public DialogBottomMenu(Context context) {
        this(context, "");
    }

    public DialogBottomMenu(Context context, String title) {
        this.context = context;
        init();
    }

    private void init() {
        popupWindow = new PopupWindow();
        contentView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_menu, null);
        listView = contentView.findViewById(R.id.rcv_msg);
        tvTitle = contentView.findViewById(R.id.tv_title);

        items = new ArrayList<>();
    }

    public DialogBottomMenu setTitle(String title) {
        if(title != null && !title.isEmpty()) {
            tvTitle.setVisibility(View.VISIBLE);
            tvTitle.setText(title);
        } else {
            tvTitle.setVisibility(View.GONE);
        }
        return this;
    }

    public DialogBottomMenu setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        return this;
    }

    public DialogBottomMenu build() {
        mAdapter = new ArrayAdapter(context, R.layout.simple_list_item, items);
        listView.setAdapter(mAdapter);
        if(onItemClickListener != null) {
            listView.setOnItemClickListener(onItemClickListener);
        }
        //点击空白处时，隐藏掉pop窗口
        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);
        popupWindow.setContentView(contentView);
        popupWindow.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(R.style.ipopwindow_anim_style);
        //添加dismiss监听，将透明度恢复
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                backgroundAlpha(1.0f);
            }
        });

        return this;
    }

    public DialogBottomMenu addItem(String item) {
        items.add(item);
        return this;
    }

    public DialogBottomMenu setItems(String[] items) {
        this.items = new ArrayList<>(Arrays.asList(items));
        return this;
    }

    public void show(View parent) {
        popupWindow.showAtLocation(parent, Gravity.BOTTOM, 0, -10);
        backgroundAlpha(0.5f);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }

    public void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = ((Activity) context).getWindow().getAttributes();
        lp.alpha = bgAlpha;
        ((Activity) context).getWindow().setAttributes(lp);
    }

}
