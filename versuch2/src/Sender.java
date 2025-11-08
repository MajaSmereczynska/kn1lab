import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Die "Klasse" Sender liest einen String von der Konsole und zerlegt ihn in einzelne Worte. Jedes
 * Wort wird in ein einzelnes {@link Packet} verpackt und an das Medium verschickt. Erst nach dem
 * Erhalt eines entsprechenden ACKs wird das nächste {@link Packet} verschickt. Erhält der Sender
 * nach einem Timeout von einer Sekunde kein ACK, überträgt er das {@link Packet} erneut.
 */
public class Sender {
    /**
     * Hauptmethode, erzeugt Instanz des {@link Sender} und führt {@link #send()} aus.
     * @param args Argumente, werden nicht verwendet.
     */
    public static void main(String[] args) {
        Sender sender = new Sender();
        try {
            sender.send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Erzeugt neuen Socket. Liest Text von Konsole ein und zerlegt diesen. Packt einzelne Worte in
     * {@link Packet} und schickt diese an Medium. Nutzt {@link SocketTimeoutException}, um eine
     * Sekunde auf ACK zu warten und das {@link Packet} ggf. nochmals zu versenden.
     * @throws IOException Wird geworfen falls Sockets nicht erzeugt werden können.
     */
    private void send() throws IOException {
        // Create a BufferedReader to read text from the standard input (console).
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter a sentence to send: ");
        String line = consoleReader.readLine();
        // Split the input line into an array of words based on one or more
        // whitespace characters ("\\s+").
        String[] words = line.split("\\s+");

        // To simplify the logic, we create a List of all messages to send
        List<String> messagesToSend = new ArrayList<>(Arrays.asList(words));
        // and then add the "EOT" (End of Transmission) string as the very last message
        messagesToSend.add("EOT");

        // Socket erzeugen auf Port 9998 und Timeout auf eine Sekunde setzen
        final int MEDIUM_PORT = 9997; // Port to send data TO (Medium's listening port)
        final int SENDER_PORT = 9998; // Port to listen FOR ACKs ON
        final int TIMEOUT_MS = 1000; // 1000 milliseconds = 1 second

        // We must use the *same socket* for sending and receiving.
        // Therefore, we create a DatagramSocket bound to the port we expect ACKs on
        DatagramSocket clientSocket = new DatagramSocket(SENDER_PORT);

        // Set the socket timeout. If clientSocket.receive() doesn't get a packet
        // within this time, it will throw a SocketTimeoutException
        clientSocket.setSoTimeout(TIMEOUT_MS);

        // Get the IP address of the Medium. Since it's running on the same machine, use "localhost"
        InetAddress IPAddress = InetAddress.getByName("localhost");

        // We need a sequence number to keep track of packets.
        int sequenceNumber = 0;

        // Loop through every message in our list (all words + "EOT").
        for (String message : messagesToSend) {
            boolean ackReceived = false;
            byte[] payload = message.getBytes();
            int expectedAck = sequenceNumber + payload.length;

            while (!ackReceived) {
                // Create a Packet with the current sequence number and payload
                // seq: The current sequence number (0 or 1).
                // ackNum: 0 (we are sending data, not an ACK, so this is irrelevant).
                // ackFlag: false (this is a data packet, not an ACK packet).
                // payload: The word (or "EOT") as bytes.
                Packet packetOut = new Packet(sequenceNumber, 0, false, payload);

                // Serialize the Packet into a byte array to be sent over UDP
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                ObjectOutputStream o = new ObjectOutputStream(b);
                o.writeObject(packetOut);
                byte[] sendBuf = b.toByteArray();

                // Create a DatagramPacket to send the serialized Packet to the Medium
                DatagramPacket sendPacket =
                    new DatagramPacket(sendBuf, sendBuf.length, IPAddress, MEDIUM_PORT);
                clientSocket.send(sendPacket);
                System.out.println("SENT: '" + message + "' (SEQ: " + sequenceNumber + ")");

                try {
                    // Prepare to receive an ACK
                    byte[] receiveBuf = new byte[1024];
                    DatagramPacket receivePacket =
                        new DatagramPacket(receiveBuf, receiveBuf.length);

                    // Wait for an ACK (this will block until we receive a packet or timeout)
                    clientSocket.receive(receivePacket);

                    // Deserialize the received packet
                    ByteArrayInputStream bi = new ByteArrayInputStream(receivePacket.getData());
                    ObjectInputStream oi = new ObjectInputStream(bi);
                    Packet packetIn = (Packet) oi.readObject();

                    // We must check TWO things:
                    // 1. Is it actually an ACK packet? (packetIn.isAckFlag())
                    // 2. Is it the ACK for the packet we just sent? (packetIn.getAckNum() ==
                    // sequenceNumber)
                    if (packetIn.isAckFlag() && packetIn.getAckNum() == expectedAck) {
                        // SUCCESS!
                        System.out.println("RECV: Correct ACK for SEQ: " + packetIn.getAckNum());
                        ackReceived = true; // This will break the 'while' loop.
                        sequenceNumber = expectedAck; // Update sequence number for next packet
                    } else {
                        // This is a "bad" ACK. It might be a duplicate ACK for a
                        // previous packet or corrupted. We ignore it.
                        System.out.println("RECV: Wrong/Corrupt ACK. Expected ACK: " + expectedAck
                            + ", Got ACK: " + packetIn.getAckNum() + ". Ignoring.");
                        // ackReceived remains false, so the 'while' loop will repeat,
                        // re-sending the packet.
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Error: Could not deserialize received packet.");
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    System.out.println("Receive timed out, retrying...");
                }
            }
        }

        System.out.println("All messages (including EOT) sent and acknowledged. Closing socket.");
        // Wenn alle Packete versendet und von der Gegenseite bestätigt sind, Programm beenden
        clientSocket.close();

        if (System.getProperty("os.name").equals("Linux")) {
            clientSocket.disconnect();
        }

        System.exit(0);
    }
}
