
package com.yyxu.download.activities;

import com.yyxu.download.R;
import com.yyxu.download.services.DownloadTask;
import com.yyxu.download.services.TrafficCounterService;
import com.yyxu.download.utils.MyIntents;
import com.yyxu.download.utils.StorageUtils;
import com.yyxu.download.utils.Utils;
import com.yyxu.download.widgets.DownloadListAdapter;
import com.yyxu.download.widgets.ViewHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

public class DownloadListActivity extends Activity {

    private ListView downloadList;
    private Button addButton;
    private Button pauseButton;
    private Button deleteButton;
    private Button trafficButton;

    private DownloadListAdapter downloadListAdapter;
    private MyReceiver mReceiver;

    private int urlIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.download_list_activity);

        if (!StorageUtils.isSDCardPresent()) {
            Toast.makeText(this, "未发现SD卡", Toast.LENGTH_LONG).show();
            return;
        }

        if (!StorageUtils.isSdCardWrittenable()) {
            Toast.makeText(this, "SD卡不能读写", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            StorageUtils.mkdir();
        } catch (IOException e) {
            e.printStackTrace();
        }

        downloadList = (ListView) findViewById(R.id.download_list);
        downloadListAdapter = new DownloadListAdapter(this);
        downloadList.setAdapter(downloadListAdapter);

        addButton = (Button) findViewById(R.id.btn_add);
        pauseButton = (Button) findViewById(R.id.btn_pause_all);
        deleteButton = (Button) findViewById(R.id.btn_delete_all);
        trafficButton = (Button) findViewById(R.id.btn_traffic);

        addButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // downloadManager.addTask(Utils.url[urlIndex]);
                Intent downloadIntent = new Intent("com.yyxu.download.services.IDownloadService");
                downloadIntent.putExtra(MyIntents.TYPE, MyIntents.Types.ADD);
                downloadIntent.putExtra(MyIntents.URL, Utils.url[urlIndex]);
                startService(downloadIntent);

                urlIndex++;
                if (urlIndex >= Utils.url.length) {
                    urlIndex = 0;
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Intent downloadIntent = new
                // Intent("com.yyxu.download.services.IDownloadService");
                // downloadIntent.putExtra(MyIntents.TYPE,
                // MyIntents.Types.STOP);
                // startService(downloadIntent);
                //
                // Intent trafficIntent = new Intent(DownloadListActivity.this,
                // TrafficCounterService.class);
                // stopService(trafficIntent);
            }
        });

        trafficButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DownloadListActivity.this, TrafficStatActivity.class);
                startActivity(intent);
            }
        });

        // downloadManager.startManage();
        Intent trafficIntent = new Intent(this, TrafficCounterService.class);
        startService(trafficIntent);

        Intent downloadIntent = new Intent("com.yyxu.download.services.IDownloadService");
        downloadIntent.putExtra(MyIntents.TYPE, MyIntents.Types.START);
        startService(downloadIntent);

        // // handle intent
        // Intent intent = getIntent();
        // handleIntent(intent);
        mReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.yyxu.download.activities.DownloadListActivity");
        registerReceiver(mReceiver, filter);

    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            handleIntent(intent);

        }

        private void handleIntent(Intent intent) {

            if (intent != null
                    && intent.getAction().equals(
                            "com.yyxu.download.activities.DownloadListActivity")) {
                int type = intent.getIntExtra(MyIntents.TYPE, -1);
                String url;

                switch (type) {
                    case MyIntents.Types.ADD:
                        url = intent.getStringExtra(MyIntents.URL);
                        boolean isPaused = intent.getBooleanExtra(MyIntents.IS_PAUSED, false);
                        if (!TextUtils.isEmpty(url)) {
                            downloadListAdapter.addItem(url, isPaused);
                        }
                        break;
                    case MyIntents.Types.COMPLETE:
                        url = intent.getStringExtra(MyIntents.URL);
                        if (!TextUtils.isEmpty(url)) {
                            downloadListAdapter.removeItem(url);
                        }
                        break;
                    case MyIntents.Types.PROCESS:
                        url = intent.getStringExtra(MyIntents.URL);
                        View taskListItem = downloadList.findViewWithTag(url);
                        ViewHolder viewHolder = new ViewHolder(taskListItem);
                        viewHolder.setData(url, intent.getStringExtra(MyIntents.PROCESS_SPEED),
                                intent.getStringExtra(MyIntents.PROCESS_PROGRESS));
                        break;
                    case MyIntents.Types.ERROR:
                        url = intent.getStringExtra(MyIntents.URL);
                        int errorCode = intent.getIntExtra(MyIntents.ERROR_CODE,
                                DownloadTask.ERROR_UNKONW);
                        handleError(url, errorCode);
                        break;
                    default:
                        break;
                }
            }
        }

        private void handleError(String url, int code) {

            switch (code) {
                case DownloadTask.ERROR_BLOCK_INTERNET:
                case DownloadTask.ERROR_UNKOWN_HOST:
                    showAlert("错误", "无法连接网络");
                    View taskListItem = downloadList.findViewWithTag(url);
                    ViewHolder viewHolder = new ViewHolder(taskListItem);
                    viewHolder.onPause();
                    break;
                case DownloadTask.ERROR_FILE_EXIST:
                    showAlert("", "文件已经存在，取消下载");
                    break;
                case DownloadTask.ERROR_SD_NO_MEMORY:
                    showAlert("错误", "存储卡空间不足");
                    break;
                case DownloadTask.ERROR_UNKONW:

                    break;
                case DownloadTask.ERROR_TIME_OUT:
                    showAlert("错误", "连接超时，请检查网络后重试");
                    View timeoutItem = downloadList.findViewWithTag(url);
                    ViewHolder timeoutHolder = new ViewHolder(timeoutItem);
                    timeoutHolder.onPause();
                    break;

                default:
                    break;
            }
        }

        private void showAlert(String title, String msg) {

            new AlertDialog.Builder(DownloadListActivity.this).setTitle(title).setMessage(msg)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                        }
                    }).create().show();
        }
    }

}
