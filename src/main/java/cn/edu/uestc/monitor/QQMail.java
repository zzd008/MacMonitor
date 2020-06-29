package cn.edu.uestc.monitor;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.sun.mail.util.MailSSLSocketFactory;


/**
 * 通过qq邮箱往163或者是qq邮箱发送邮件
 */
public class QQMail {

    private String topic;//主题
    private String context;//邮件正文
    private List<String> receivers;//收件人邮箱列表

    public QQMail(String topic, String context, List<String> receivers) {
        this.topic = topic;
        this.context = context;
        this.receivers = receivers;
    }

    public boolean sendMails(){
        Properties props = new Properties();

        // 开启debug调试
        //props.setProperty("mail.debug", "true");
        // 发送服务器需要身份验证
        props.setProperty("mail.smtp.auth", "true");
        // 设置邮件服务器主机名
        props.setProperty("mail.host", "smtp.qq.com");
        // 发送邮件协议名称
        props.setProperty("mail.transport.protocol", "smtp");

        try {
            //qq邮箱要开启ssl
            MailSSLSocketFactory sf = new MailSSLSocketFactory();//pom中要导入1.4.7的版本才有MailSSLSocketFactory这个类
            sf.setTrustAllHosts(true);
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.socketFactory", sf);

            Session session = Session.getInstance(props);

            //设置message对象
            Message msg = new MimeMessage(session);
            msg.setSubject(topic);//主题
            msg.setText(context);
            msg.setFrom(new InternetAddress("1311234375@qq.com"));//发件人的邮箱

            Transport transport = session.getTransport();
            transport.connect("smtp.qq.com", "1311234375@qq.com", "upldxiwbwbmsjjfc");//发件人的邮箱，授权码(会变，要去qq中设置)，不能用明文密码

            //遍历，发送邮件至多人
            for(String mail:receivers){
                //transport.sendMessage(msg, new Address[] { new InternetAddress("1311234375@qq.com") });//收件人邮箱,qq邮箱
                transport.sendMessage(msg, new Address[] { new InternetAddress(mail) });//收件人邮箱,163邮箱
            }
            transport.close();
            System.out.println("------邮件发送成功!-------");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
