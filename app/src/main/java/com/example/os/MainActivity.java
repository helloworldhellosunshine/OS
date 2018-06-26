package com.example.os;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private TextView tv_running_process;
    private ProgressBar process;
    private ImageView iv_idle;


    private ListView lv_ready;
    private ListView lv_block;


    private EditText et_input;
    private Button bt_add;
    private TextView tv_running_code;
    private TextView tv_running_results;
    private ListView lv_results;

    private List<PCB> readyQueue;
    private List<PCB> blockQueue;
    private List<PCB> resultsQueue;

    private boolean isRunning = false;

    private PCB runningProcess;

    private int id = 1;

    private String[] code;

    private ReadyAdapter readyAdapter;
    private BlockAdapter blockAdapter;
    private ResultsAdapter resultsAdapter;

    private int  timefile=5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除顶部标题栏
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // 初始化控件
        initView();

        // 初始化点击事件
        initClick();


        // 初始化就绪队列
        initReadyQueue();

        // 初始化阻塞队列
        initBlockQueue();

        // 初始化结果队列
        initResults();

        //对CPU运行状态进行检测
        detection();

        // cpu函数
        CPU();

    }

    //对CPU运行状态进行检测
    private void detection() {
        new Thread(){
            @Override
            public void run() {
                super.run();
                try{
                    while (true){
                        Thread.sleep(1000);

                        // 检查当前是否是空闲状态
                        checkIdle();

                        // 检查就绪队列
                        checkReady();

                        // 检查阻塞队列
                        checkBlock();

                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            // 检查当前是否是空闲状态
            private void checkIdle() {
                if (readyQueue.isEmpty() && blockQueue.isEmpty() && !isRunning) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //轮转的时间片
                            iv_idle.setVisibility(View.VISIBLE);
                            process.setVisibility(View.GONE);
                        }
                    });
                }
            }

            // 检查就绪队列
            private void checkReady() {
                // 如果当前就绪队列不为空 并且没有正在执行的进程 就从就绪队列中取出一个进程执行
                if (!readyQueue.isEmpty() && !isRunning) {

                    runningProcess = readyQueue.get(0);
                    readyQueue.remove(0);

                    // 更新执行状态
                    isRunning = true;

                    // 根据分号将字符串分割成字符串数组 方便解析代码
                    code = runningProcess.IR.split(";");
                    if (!code[code.length - 1].equals("end")) {
                        runningProcess.IR = runningProcess.IR + ";end";
                        code = runningProcess.IR.split(";");
                    }

                    // 更新界面
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (runningProcess != null) {
                                tv_running_process.setText("id：" + runningProcess.id);
                            }

                            readyAdapter.notifyDataSetChanged();

                            iv_idle.setVisibility(View.GONE);
                            process.setVisibility(View.VISIBLE);
                        }
                    });

                    if (code.length <= 0) {
                        erroCode();
                    }
                }
            }

            // 检查阻塞队列
            private void checkBlock() {
                // 如果阻塞队列不为空 就一直更新阻塞队列里的阻塞时间
                if (!blockQueue.isEmpty()) {
                    for (int i = 0; i < blockQueue.size(); i++) {
                        blockQueue.get(i).time--;

                        // 更新界面
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                blockAdapter.notifyDataSetChanged();
                            }
                        });

                        // 当一个进程的阻塞时间小于零后  将该进程唤醒
                        if (blockQueue.get(i).time < 0) {
                            wakeup(i);
                        }
                    }
                }
            }

        }.start();
    }

    // cpu函数
    private void CPU() {
        // 开一个子线程 一直循环
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    while (true) {
                        // 每隔1秒执行一次
                        Thread.sleep(1000);

                        // 解析运行进程代码
                        runningCode();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 运行并解析进程代码
            private void runningCode() {
                // 当前正在执行程序  进行代码的解析操作
                if (runningProcess != null && runningProcess.PC < code.length) {
                    // 解析代码
                    exeCode(code[runningProcess.PC],5);
                    // 更新界面
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (runningProcess != null && runningProcess.PC < code.length) {
                                //显示正在运行的指令
                                tv_running_code.setText(code[runningProcess.PC]);
                                //显示执行的中间结果
                                tv_running_results.setText(code[0].charAt(0) + "=" + runningProcess.variables);
                            }
                        }
                    });
                }
            }

            // 解析代码
            private void exeCode(String s,int a) {
                //时间片轮转调度算法
                timefile--;
                Log.d("dada1", String.valueOf(timefile));
                if (timefile%5==0){
                    //因时间片轮转而加入就绪队列末尾
                   BeReady();
                   timefile=5;
                   Log.d("dada", String.valueOf(timefile));
                }else {
                    if(s.equals("end")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // 将结果添加到结果队列里
                                resultsQueue.add(runningProcess);
                                resultsAdapter.notifyDataSetChanged();
                                // 更新界面
                                tv_running_process.setText("");
                                tv_running_code.setText("");
                                tv_running_results.setText("");
                                // 销毁原语
                                distroy();
                                timefile=5;
                            }
                        });
                        // 阻塞语句
                    } else if (s.length() > 0 && s.charAt(0) == '!') {
                        // 给PCB的阻塞时间赋值
                        runningProcess.time = Integer.valueOf(s.substring(1));
                        // 阻塞原语
                        block();
                        timefile=5;
                    } else {
                        int st = -1;
                        if (s.contains("=")) {
                            st = 0;
                        } else if (s.contains("++")) {
                            st = 1;
                        } else if (s.contains("--")) {
                            st = 2;
                        }

                        switch (st) {
                            case 0:
                                try {
                                    runningProcess.variables = Integer.valueOf(s.substring(s.indexOf("=") + 1));
                                } catch (NumberFormatException e) {
                                    erroCode();
                                }
                                break;
                            case 1:
                                runningProcess.variables = runningProcess.variables + 1;
                                break;
                            case 2:
                                runningProcess.variables = runningProcess.variables - 1;
                                break;
                            // 在输入非法语句时执行
                            default:
                                erroCode();
                        }
                    }
                    // 更新PC程序指针 下次取下一条指令
                    if (runningProcess != null) {
                        runningProcess.PC++;
                    }
                }
            }

        }.start();
    }

    // 输入进程代码有误
    public void erroCode() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runningProcess.variables = -1024;
                resultsQueue.add(runningProcess);
                resultsAdapter.notifyDataSetChanged();

                tv_running_process.setText(" ");
                tv_running_code.setText(" ");
                tv_running_results.setText(" ");

                distroy();

                Toast.makeText(getApplicationContext(), "输入进程内容有误，请重新输入", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 创建PCB
    private void create(String IR, int i) {
        // 创建进程控制块，并初始化
        PCB pcb = new PCB();
        pcb.id = id;
        pcb.IR = IR;
        pcb.variables = 0;
        pcb.PSW = PCB.STATUS_READY;
        pcb.time = 0;
        pcb.PC = 0;

        // 插入到就绪队列
        readyQueue.add(pcb);

        // 刷新界面
        readyAdapter.notifyDataSetChanged();

        // 进程数加一
        id++;


    }

    // 撤销PCB
    private void distroy() {
        // 将正在运行标志更新
        isRunning = false;
        // 释放资源
        runningProcess = null;
        // 进程数减一
        id--;
    }

    // 阻塞PCB
    private void block() {
        // 更改运行中的PCB状态
        runningProcess.PSW = PCB.STATUS_BLOCK;
        runningProcess.PC++;

        // 将其添加到阻塞队列中
        blockQueue.add(runningProcess);

        // 更新界面
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_running_process.setText(" ");
                blockAdapter.notifyDataSetChanged();
            }
        });

        // 更新执行状态
        isRunning = false;
        runningProcess = null;
    }

    // 唤醒PCB
    private void wakeup(int i) {
        // 从阻塞队列中移除一个进程
        PCB pcb = blockQueue.get(i);
        pcb.PSW = PCB.STATUS_READY;
        blockQueue.remove(i);

        // 添加到就绪队列
        readyQueue.add(pcb);

        // 更新界面
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                blockAdapter.notifyDataSetChanged();
                readyAdapter.notifyDataSetChanged();
            }
        });
    }

    //因时间片轮转而加入就绪队列末尾
    private void BeReady(){
        runningProcess.PSW=PCB.STATUS_EXECUTE;
        //将其添加到就绪队列
        readyQueue.add(runningProcess);

        // 将正在运行标志更新
        isRunning = false;
        // 释放资源
        runningProcess = null;

        //更新页面
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                readyAdapter.notifyDataSetChanged();
            }
        });
    }

    // 初始化就绪队列
    private void initReadyQueue() {
        readyQueue = new ArrayList<>();
        readyAdapter = new ReadyAdapter(this, readyQueue);
        lv_ready.setAdapter(readyAdapter);
    }

    // 初始化阻塞队列
    private void initBlockQueue() {
        blockQueue = new ArrayList<>();
        blockAdapter = new BlockAdapter(this, blockQueue);
        lv_block.setAdapter(blockAdapter);
    }

    // 初始化结果队列
    private void initResults() {
        resultsQueue = new ArrayList<>();
        resultsAdapter = new ResultsAdapter(this, resultsQueue);
        lv_results.setAdapter(resultsAdapter);
    }

    // 初始化点击事件
    private void initClick() {
        // 添加进程按钮点击事件
        bt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String IR = et_input.getText().toString().trim();
                if (!TextUtils.isEmpty(IR)) {
                    if (id <= 10) {
                        create(IR,5);
                    } else {
                        Toast.makeText(MainActivity.this, "内存已满", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "请将信息填写完整", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    // 初始化控件
    private void initView() {
        tv_running_process = (TextView) findViewById(R.id.tv_running_process);
        process = (ProgressBar) findViewById(R.id.process);
        iv_idle = (ImageView) findViewById(R.id.iv_idle);

        lv_ready = (ListView) findViewById(R.id.lv_ready);
        lv_block = (ListView) findViewById(R.id.lv_block);

        et_input = (EditText) findViewById(R.id.et_input);
        bt_add = (Button) findViewById(R.id.bt_add);

        tv_running_code = (TextView) findViewById(R.id.tv_running_code);

        tv_running_results = (TextView) findViewById(R.id.tv_running_results);

        lv_results = (ListView) findViewById(R.id.lv_results);
    }

}

