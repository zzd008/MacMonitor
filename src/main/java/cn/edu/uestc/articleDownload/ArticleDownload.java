package cn.edu.uestc.articleDownload;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
/**
 * 论文下载：dblp 输入论文搜索后的链接，自动下载
 * 存在问题：dblp是动态下拉来分页的，Jsoup无法获取全部的数据
 * Jsoup指南：https://www.open-open.com/jsoup/
 */
public class ArticleDownload {
    public static void main(String[] args){
        //url
        String url = "https://dblp.uni-trier.de/search?q=Proxy%20Re-encryption%20year%3A2019%3A";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements articles = doc.select("[class=entry article toc]");//所有搜索出来的文章
            System.out.println(articles.size());
            int counter = 1;//下载文件名递增
            for (Element article:articles) {//对每一篇文章处理
                //获取doi url
                String doiUrl = article.select("[class=drop-down]").get(1).select("[class=head]").select("a[href]").get(0).attr("href");
                Document doiDoc = Jsoup.connect(doiUrl).get();
                //获取doi所在的字符串
                String doiStr = doiDoc.select("[class=verbatim select-on-click]").get(0).text();
                //截取doi
                int firstIndex = doiStr.indexOf("doi       = {");
                int lastIndex = doiStr.indexOf("}",firstIndex);
                String doi = doiStr.substring(firstIndex + 13,lastIndex);
                //sci-hub网址
                String sci_hub = "https://sci-hub.si/";
                //sci-hub中论文地址=sci-hub+doi
                String sci_hubUrl = sci_hub + doi;
                Document sciDoc = Jsoup.connect(sci_hubUrl).get();
                //解析下载地址
                String downloadStr = sciDoc.getElementById("buttons").select("a[href]").get(0).attr("onclick");
                //截取
                int beginIndex = downloadStr.indexOf("'") + 1;
                int endIndex = downloadStr.length() - 1;
                String downloadUrl = downloadStr.substring(beginIndex,endIndex);
                if(!downloadUrl.contains("https:")){
                    downloadUrl = "https:" + downloadUrl;
                }

                System.out.println(sci_hubUrl);
                System.out.println(downloadUrl);
                System.out.println("第" + counter + "篇正在下载中");

                //下载路径
                String filePath = "/Users/zhouzhida/Downloads/t/" + counter++ + ".pdf";
                //下载
                downLoad(downloadUrl,filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void downLoad(String downloadUrl,String filePath) {
        try {
            URL url  = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            //设置超时间为30秒
//        conn.setConnectTimeout(30*1000);
            //输入输出流
            BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));

            byte[] buf = new byte[4096];
            int length = bis.read(buf);
            //下载文件
            while(length != -1)
            {
                bos.write(buf, 0, length);
                length = bis.read(buf);
            }
            bos.close();
            bis.close();
            conn.disconnect();
            System.out.println("下载成功！");
        } catch (Exception e) {
            System.err.println("下载失败！");
            e.printStackTrace();
        }

    }
}
