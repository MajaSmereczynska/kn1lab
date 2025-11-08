import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

public class Receive_Mail {
	public static void main(String[] args) throws Exception {
		fetchMail();
	}
	
	public static void fetchMail() {
		// We declare Store and Folder here as null.
		// This is important so we can access them in the "finally" block
		// to close them, even if an error happens inside the "try" block
		Store store = null;
		Folder emailFolder = null;
		
		try {
			// Define Connection Parameters
			String host = "localhost";
			String user = "labrat";
			String password = "kn1lab";
			String storeType = "pop3";

            // Set system properties for the session
            // We tell the Java Mail API where to find the POP3 (receiving) server
            Properties properties = System.getProperties();
            properties.setProperty("mail.pop3.host", host);

			// Get a Mail Session
			// The Session object coordinates all mail activities
			Session session = Session.getDefaultInstance(properties);
			
			// Connect to the Store
			// A "Store" is the service that stores emails (like a POP3 server)
			store = session.getStore(storeType);
			System.out.println("Connecting to POP3 server on " + host + "...");
			
			// This is the actual login.
			store.connect(host, user, password);
			System.out.println("Connection successful!");

			// Open the INBOX Folder
			// A "Folder" is where messages are held. For POP3, it's always "INBOX"
			emailFolder = store.getFolder("INBOX");
			
			// We open the folder in READ_ONLY mode because we just want to look, not make changes
			emailFolder.open(Folder.READ_ONLY);

			// Fetch and Loop Through Messages
			// Get all messages from the folder and put them in an array
			Message[] messages = emailFolder.getMessages();
			System.out.println("Emails in INBOX: " + messages.length);
			System.out.println("----------------------------------------\n");

			// Loop through every message in the array.
			for (int i = 0; i < messages.length; i++) {
				Message message = messages[i];
				
				// Print Details (as required by the assignment)
				
				// Requirement: Numbering
				System.out.println("--- EMAIL #" + (i + 1) + " ---");
				
				// Requirement: Sender
				if (message.getFrom() != null && message.getFrom().length > 0) {
					System.out.println("From: " + message.getFrom()[0].toString());
				}
				
				// Requirement: Subject
				System.out.println("Subject: " + message.getSubject());
				
				// Requirement: Sent Date
				System.out.println("Sent Date: " + message.getSentDate());
				
				// Requirement: Content
				System.out.println("Content: " + message.getContent().toString());
				System.out.println("\n");
			}

            store.close();

		} catch (Exception e) {
			// If anything goes wrong (bad password, server down), print the error.
			e.printStackTrace();
		}
	}
}
