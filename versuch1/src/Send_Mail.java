import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage; 

public class Send_Mail {
	public static void main(String[] args) {
		sendMail();   
	}
	
	public static void sendMail() {
		try {
            // Define the host
            String host = "localhost";

            // Set system properties for the session
            // We tell the Java Mail API where to find the SMTP (sending) server
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", host);

            // Get a session instance without authentication
            // Since the server is on localhost, we don't need an authenticator (username/password)
            Session session = Session.getDefaultInstance(properties);

            // Create a new MimeMessage object
            MimeMessage message = new MimeMessage(session);

            // Set the "From" address (must end in @localhost)
            message.setFrom(new InternetAddress("labrat@localhost"));

            // Set the "To" recipient (to labrat@localhost)
            message.setRecipient(Message.RecipientType.TO, new InternetAddress("labrat@localhost"));

            // Set the email subject
            message.setSubject("Test Mail for KN1 Exercise 1");

            // Set the actual text content of the email
            message.setText("This is a second test email sent from the Java Mail API.");

            // Send the email using the Transport class
            Transport.send(message);

            System.out.println("Email sent successfully!");
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
}
