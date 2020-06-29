package cn.edu.uestc.monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * mac地址监控 通过nmap进行地址扫描，然后判断指定的mac地址是否存在，并判断到来和离开时间
 * linux安装nmap -> 获取指定手机mac地址 -> 扫描局域网mac -> 监控
 */
public class Monitor {
    private static boolean beforeStatus = false;
    private static boolean isFirst = true;
    private static boolean check = false;//检测nmap漏扫
    private static int count = 0;

    public static void main(String[] args) {

        final String mac = args[0];
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    boolean currentStatus = false;

                    String cmd = "nmap -sP  192.168.1.0/24 | grep " + mac;
                    String[] exe = new String[] { "/bin/sh", "-c", cmd };//等待命令执行完成

                    Process ps = Runtime.getRuntime().exec(exe);
                    BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println(line);
                        if(line.contains(mac)){
                            currentStatus = true;//在线
                            break;
                        }
                    }
                    if(!currentStatus){//打印提示
                        System.out.println("不在线！");
                    }
                    if(isFirst){
                        beforeStatus = currentStatus;
                        isFirst = false;
                    }
                    if(currentStatus){//现在在线
                        if(!beforeStatus){//之前不在线
                            String tip = "！";
                            List<String> receivers = new ArrayList<String>();//收件人邮箱列表
                            receivers.add("1311234375@qq.com");
                            new QQMail("出现！！！",tip ,receivers).sendMails();
                            beforeStatus = currentStatus;
                        }
                        if(check){
                            count = 0;//开始检测后又可以扫到说明是漏扫了，计数归零,停止检测
                            check = false;
                        }
                    }else{//现在不在线
                        if(beforeStatus){//之前在线
                            check = true;//开始检测
                            count++;
                            if(count == 20){//连续20次全部都不在线才认为真正离开，防止nmap漏扫(离开20分钟才发邮件)
                                String tip = "！";
                                List<String> receivers = new ArrayList<String>();//收件人邮箱列表
                                receivers.add("1311234375@qq.com");
                                new QQMail("消失！！",tip ,receivers).sendMails();
                                beforeStatus = currentStatus;
                                check = false;//关闭检测
                                count = 0;
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 在1秒之后开始执行，每x秒执行一次
        }, 1000, 1000 * 60);//60s扫一次
    }
}
