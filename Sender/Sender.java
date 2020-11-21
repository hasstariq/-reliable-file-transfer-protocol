import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Sender {
    public static void main(String[] args) throws Exception {
        if (args.length != 6){
            throwException();
        }else {
            validateArguments(args);
        }

        Instant start = null;
        //Create the socket and set the timeout
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[2]));
        socket.setSoTimeout(Integer.parseInt(args[5])/1000);
        //Get the file (this doesn't get the content)
        String currentDir = Paths.get(System.getProperty("user.dir")).toString();
        URI path;
        if (currentDir.endsWith("src")){
           path = Paths.get(currentDir, args[3]).toUri();
        }else {
            path = Paths.get(currentDir, "src", args[3]).toUri();
        }

        File file = new File(path);
        if (!file.exists()){
            throwException();
        }
        //Create a stream to read the content of the file
        FileInputStream targetStream = new FileInputStream(file);
        //Get the MDS
        int mds = Integer.parseInt(args[4]);
        //Get the file size
        int remainingFileSize=targetStream.available();
        //Initialise counters
        int packetCounter = 1;
        int lostPackets = 0;
        //Keep going until we send the entire file
        while (remainingFileSize > 0){
            System.out.println("Current filesize: " + remainingFileSize);
            byte[] buffer = new byte[mds - 10];
            int actualAmountOfBytes;
            //We calculate data size by taking the mds and removing 10 bytes form it for the sequence number;
            if (remainingFileSize <= mds - 10){
                actualAmountOfBytes = targetStream.readNBytes(buffer, 0, remainingFileSize);
            }else {
                actualAmountOfBytes = targetStream.readNBytes(buffer, 0, mds-10);
            }
            //Copy the buffer. This is to make sure that the inner methods don't mess up our data.
            byte[] copiedBuffer = Arrays.copyOfRange(buffer, 0, actualAmountOfBytes);
            try {
                //Create and send packets until an ACK is received.
                while(!createAndSendPacketAndAck(args[0], args[1], socket, copiedBuffer, packetCounter)){
                    System.out.println("Packet #" + packetCounter +" lost");
                    copiedBuffer = Arrays.copyOfRange(buffer, 0, actualAmountOfBytes);
                    //We only want to count the lost packets when sending has started
                    if (packetCounter > 1 ){
                        lostPackets++;
                    }
                }
                //We will keep track of time from the moment we send the first packet successfully
                if (packetCounter == 1){
                    start = Instant.now();
                }
                //Calculate the remaining filesize and add 1 to the packet counter
                remainingFileSize -= buffer.length;
                packetCounter++;
            }catch (Exception e){
                //Unnecessary try-catch but if we want to change how the ACK system works then this is a possible route.
                throw new Exception("Host " + args[0] + ":" + args[1] + " is not available or does not exist.");
            }
        }
        //We are done sending data so close the filestream.
        targetStream.close();
        //Send the EoT and await the ACK.
        sendEndOfTransmission(args[0], args[1], socket);
        //Get the current time to calculate execution time and start logging important data.
        Instant end = Instant.now();
        System.out.println("MDS: " + args[4]);
        System.out.println("Timeout in µs: " + args[5]);
        System.out.printf("Send time: %d ms%n", Duration.between(start, end).toMillis());
        System.out.println("Send packets: " + packetCounter);
        System.out.println("Lost packets: " + lostPackets);
    }

    private static void sendEndOfTransmission(String destIp, String destPort,DatagramSocket socket) throws IOException {
        System.out.println("Sending EoT");
        byte[] endOfTransmissionFlag = "EoT".getBytes(StandardCharsets.UTF_8);

        while (!createAndSendPacketAndAck(destIp, destPort, socket, endOfTransmissionFlag, 0)){
            System.out.println("Sending EoT failed");
        }
    }
    //This method handles the sending of packets and the awaiting of the ACK
    private static boolean createAndSendPacketAndAck(String destIp, String destPort,DatagramSocket socket, byte[] data, int sequence) throws IOException {
        socket.send(createPacket(sequence, data, destIp, destPort));

        try {
            DatagramPacket ackPacket = new DatagramPacket(new byte[3], 3);
            socket.receive(ackPacket);
            String ack = new String(ackPacket.getData(),0, ackPacket.getLength(), StandardCharsets.UTF_8);
            return "ACK".equals(ack);
        }catch (Exception ignored){
            return  false;
        }

    }
    //This method will validate all arguments
    private static void validateArguments(String[] args) throws Exception {
        if (!validateIpAddress(args[0]) && !isInteger(args[1]) && !isInteger(args[2]) && !isInteger(args[4]) && !isInteger(args[5])){
            throwException();
        }
    }
    //Helper method to create a packet in a uniform manner.
    private static DatagramPacket createPacket(int sequenceNumber, byte[] data, String ipaddress, String port) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(outputStream);
        stream.writeInt(sequenceNumber);
        stream.write(data);
        return new DatagramPacket(outputStream.toByteArray(), outputStream.size(), InetAddress.getByName(ipaddress), Integer.parseInt(port));
    }

    //This exception shows the arguments that the user needs to give.
    private static void throwException() throws Exception{
        String[] arguments = new String[6];
        arguments[0] = "host address of the receiver";
        arguments[1] = "UDP port number used by the receiver to receive data from the sender";
        arguments[2] = "UDP port number used by the sender to receive ACKs from the receiver";
        arguments[3] = "name of the file to be transferred";
        arguments[4] = "the maximum size of data inside the UDP datagram";
        arguments[5] = "integer number for timeout (in microseconds)";
        String exceptionMessage = "This program requires 6 arguments:";
        for (String argument: arguments) {
            exceptionMessage += "\n\t -" + argument;
        }
        exceptionMessage += "\n In this given order. Example: java Sender 129.122.0.10 4455 3321 test.txt 400 20000";

        throw new Exception(exceptionMessage);
    }
    //This method will validate an ip address
    private static boolean validateIpAddress(String address){
        String zeroTo255 = "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])";
        String IP_REGEXP = zeroTo255 + "\\." + zeroTo255 + "\\."
                + zeroTo255 + "\\." + zeroTo255;
        Pattern pattern = Pattern.compile(IP_REGEXP);
        return pattern.matcher(address).matches();
    }

    //This method will check if an String is an Integer
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }
    //This method will check if an String is an Integer
    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
}
