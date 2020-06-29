package cn.edu.uestc.auto;

import java.io.*;
import java.util.List;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *  自动研究生健康打卡
 *  htmlunit库
 *  模拟一个浏览器客户端-->先登录信息门户网站，记录用户session-->再去打卡页面打卡
 *  信息门户：https://idas.uestc.edu.cn/authserver/login?service=http%3A%2F%2Fportal.uestc.edu.cn%2F
 *  打卡页面：http://eportal.uestc.edu.cn/qljfwapp/sys/lwReportEpidemicStu/index.do?t_s=1591584686636&amp_sec_version_=1&gid_=djg5RWNwN1JCdFV1WUh0bWZwUUxCUU9qbi9UaDR5SW1iYnhOcXFDS05vdGdOV204SVRoUGVoWnRsMEZreWxPeXJNVVJMRE1CQ3ErSFMrNlZvWDAvTnc9PQ&EMAP_LANG=zh&THEME=indigo#/dailyReport
 *  如果直接访问打卡页面，因为未登录，会先跳转至信息门户，登录成功后无法跳转至打卡页面，因为一个是https，一个是http，会显示禁止跨域访问
 */
public class UestcLogin {
    //记录验证码多少次匹配成功
    private static int count = 1;

    public static void main(String [] args) throws FailingHttpStatusCodeException, IOException, InterruptedException {
        //信息门户url
        String loginURL = "https://idas.uestc.edu.cn/authserver/login?service=http%3A%2F%2Fportal.uestc.edu.cn%2F";
        //打卡页面rul
        String targetURL = "http://eportal.uestc.edu.cn/qljfwapp/sys/lwReportEpidemicStu/index.do?t_s=1591584686636&amp_sec_version_=1&gid_=djg5RWNwN1JCdFV1WUh0bWZwUUxCUU9qbi9UaDR5SW1iYnhOcXFDS05vdGdOV204SVRoUGVoWnRsMEZreWxPeXJNVVJMRE1CQ3ErSFMrNlZvWDAvTnc9PQ&EMAP_LANG=zh&THEME=indigo#/dailyReport";
        //先登录，缓存用户信息，然后去打卡页面打卡
        deal(loginURL, targetURL);
    }

    //处理登录、打卡
    public static void deal(String loginURL, String targetURL) throws IOException, InterruptedException {
        //模拟一个浏览器
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_60);//使用CHROME在执行waitForBackgroundJavaScript时会停不下来

        //设置webClient的相关参数
        webClient.getOptions().setJavaScriptEnabled(true); //启动JS
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false); //禁用Css，可避免自动二次请求CSS进行渲染
        webClient.setAjaxController(new NicelyResynchronizingAjaxController()); //设置Ajax
        webClient.getOptions().setTimeout(60000); //设置超时
        webClient.getOptions().setThrowExceptionOnScriptError(false); //运行错误时，是否抛出异常

        //打开目标网址
        HtmlPage loginPage = webClient.getPage(loginURL);

        //填充用户名
        HtmlElement username = (HtmlElement) loginPage.getElementById("username");
        username.click();
        username.type("201921090126");

        //填充密码
        HtmlElement password = (HtmlElement) loginPage.getElementById("password");
        password.click();
        password.type("zhouzhida007");

        //识别验证码
        String res = getCaptcha(loginPage);

        //填充验证码
        HtmlElement captcha = (HtmlElement) loginPage.getElementById("captchaResponse");
        captcha.click();
        captcha.type(res);

        //登录按钮
        HtmlButton loginBtn = (HtmlButton)loginPage.getByXPath("//*[@class='auth_login_btn primary full_width']").get(0);

        //点击登录
        HtmlPage targetPage = loginBtn.click();

        String targetPageContent = targetPage.asText();
        if(targetPageContent.contains("Invalid")){//验证码无效,重新执行该函数
            count++;
//            System.err.println("验证码无效------------------------");
            deal(loginURL, targetURL);
        }else {//有效，登录成功后session中就会有用户信息，直接去打卡页面
//            System.err.println("验证码有效------------------------，共验证次数：" + count);
            HtmlPage addPage = webClient.getPage(targetURL);

            //打卡
            //新增按钮是页面执行js之后出来的，所以要获取js执行之后的元素必须先等待js执行完毕 https://blog.csdn.net/xiaoqiangyonghu/article/details/85701653?utm_medium=distribute.pc_relevant.none-task-blog-baidujs-1
            webClient.waitForBackgroundJavaScript(30000);//阻塞线程,等待js全部执行完毕

            //获取新增按钮  极个别情况会获取不到
            List<HtmlElement> list =  addPage.getByXPath("/html/body/main/article/section/div[2]/div");

            while(list.size() <= 0){//获取不到，重新进入打卡页面并再次获取，直到获取到
               addPage = webClient.getPage(targetURL);
               webClient.waitForBackgroundJavaScript(30000);
               Thread.sleep(5000);
               list =  addPage.getByXPath("/html/body/main/article/section/div[2]/div");
            }

            //获取
            HtmlElement increaseButton = list.get(0);

            //点击新增
            HtmlPage savePage = increaseButton.click();

            //获取保存按钮
            HtmlElement saveButton = (HtmlElement)savePage.getByXPath("//*[@id=\"save\"]").get(0);

            //点击保存
            HtmlPage finalPage = saveButton.click();

            //获取确认按钮
            //这里要先停一会(不能使用waitForBackgroundJavaScript方法)，等待上次点击完成后，再次点击才能获取到弹框中的确认按钮
            Thread.sleep(6000);
            saveButton.click();

            HtmlElement sureButton = (HtmlElement)finalPage.getByXPath("//*[@class='bh-dialog-btn bh-bg-primary bh-color-primary-5']").get(0);//不能使用xpath，因为每次都会变

            //点击确认,打卡完成
            sureButton.click();
            Thread.sleep(6000);//等待结果执行完
//            System.err.println("******************************打卡成功****************************************************");
        }
    }

    /*
       识别验证码 tess4j库 https://www.cnblogs.com/zengbojia/archive/2018/08/01/9401047.html
       linux/mac使用tess4j需要安装tesseract  https://github.com/tesseract-ocr/tesseract/wiki
     */
    public static String getCaptcha(HtmlPage page) throws IOException {
        //识别结果
        String result = "";
        HtmlImage img = (HtmlImage)page.getElementById("captchaImg");//获取验证码图片
        //图片存储地址
        File file = new File("/Users/zhouzhida/Desktop/auto/img.jpg"); //linux:/root/practise_zzd/auto/img/img.jpg
        //保存图片
        img.saveAs(file);

        //识别
        Tesseract tessreact = new Tesseract();
        tessreact.setDatapath("/Users/zhouzhida/Desktop/auto/tessdata");//linux:/root/practise_zzd/auto/tessdata
        try {
            result = tessreact.doOCR(file);
            //识别结果中可能有空字符，处理掉
            result = result.replaceAll(" ","");
        } catch (TesseractException e) {
            e.printStackTrace();
        }
        return result;
    }
}

