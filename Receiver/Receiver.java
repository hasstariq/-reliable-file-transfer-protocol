
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

public class Receiver {
    private ReceiverUI ui;
    private TreeMap<Integer, byte[]> incoming;

    public Receiver() {
        ui = new ReceiverUI(this);
    }

    public static void main(String[] args) throws IOException {
        Receiver receiver = new Receiver();
    }
    public void startReceiving(int timeout, int mds,String filename, String serverId, int serverPort, int receiverPort, ReceiverMode mode) throws IOException {
        //Initialise the TreeMap
        incoming = new TreeMap<>();
        //Create the socket and set the timeout.
        DatagramSocket socket = new DatagramSocket(receiverPort);
        socket.setSoTimeout(timeout/1000);

        byte[] data;
        int packetCounter = 0;
        //Create a boolean that tracks whether or not we dropped the packet
        boolean droppedPacket  = false;
        //We want this to keep going until we are done.
        while (true) {
            try {
                //Initialise an buffer to receive the data
                data = new byte[mds];
                //Wait and receive the packet until timeout
                DatagramPacket packet = new DatagramPacket(data, mds);
                socket.receive(packet);

                //Create an input stream to get the data in a clean way
                ByteArrayInputStream inputStream =new ByteArrayInputStream(packet.getData(),0,packet.getLength());
                DataInputStream stream = new  DataInputStream(inputStream);
                int sequenceNumber = stream.readInt();
                byte[] bytes = stream.readAllBytes();
                //Convert the bytes to String (this is to check for EoT)
                String words = new String(bytes,0, bytes.length, StandardCharsets.UTF_8);
                //Drop every 10th packet
                if ((packetCounter + 1) % 10 == 0 ){
                    droppedPacket = !droppedPacket;
                }

                //Only accept packets when we are in mode: reliable OR we are in mode: unreliable && we haven't dropped the packet
                if(mode == ReceiverMode.RELIABLE || (mode == ReceiverMode.UNRELIABLE && !droppedPacket)){
                    if ("EoT".equals(words)){
                        //EoT is a packet but is not part of the file so we add the counter here
                        ui.updatePacketCounter(packetCounter + 1);
                        //Send an ACK for the EoT
                        sendAck(serverId, serverPort, socket);
                        System.out.println("Received EoT packet. #"+ (packetCounter+1));
                        //Break the loop
                        break;
                    }
                    System.out.println("Saving data...");

                    packetCounter++;
                    ui.updatePacketCounter(packetCounter);
                    //Add the packet to the TreeMap.
                    incoming.put(sequenceNumber, bytes);
                    //Send an ACK for the packet
                    sendAck(serverId, serverPort, socket);
                    System.out.println("Done with packet: " + packetCounter);
                }else {
                    System.out.println("Dropping packet: " + (packetCounter + 1));
                }
            }catch (Exception ignored){

            }

        }
        //Create and empty and byte array of size 0
        byte[] allBytes = new byte[0];
        //Concat all bytes from the TreeMap together but in order.
        for (int i = 1; i <= packetCounter; i++) {
            allBytes = Utils.concat(allBytes,incoming.get(i));
        }
        //Convert the byte array to a utf-8 String.
        String byteString = new String(allBytes, 0 ,allBytes.length, StandardCharsets.UTF_8);
        //Remove NUL values and replace EoT if the sender ever decides to include the flag in a data packet.
        String toWrite = byteString.replace("\0","").replace("EoT","");
        //Create an output stream to write to the file.
        FileOutputStream fos = new FileOutputStream(String.format("./%s.txt",filename));
        //Convert the improved String to bytes
        fos.write(toWrite.getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    private void sendAck(String senderIp, int senderPort, DatagramSocket socket) throws IOException {
        byte[] ack = "ACK".getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(ack, ack.length, InetAddress.getByName(senderIp), senderPort));
    }

}
