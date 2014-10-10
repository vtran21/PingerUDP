/*******************************************
 *PingClient.java
 *Client to send ping requests over UDP
 ******************************************/


import java.io.*;
import java.net.*;
import java.util.*;


/*
 *Server to process ping requests over UDP
 */
 
public class PingClient
{
   private static final int NUM_PINGS = 10; //must be a positive number
   private static final int TIMEOUT = 1000; // milliseconds (1 minute)
   private static final int SEND_INTERVAL = 1000; // milliseconds (1 minute)
   
   public static void main(String[] args) throws Exception
   {
      //Get command line argument
      if ((args.length != 2)) // Test for correct # of args
         throw new IllegalArgumentException("Parameter(s): <Host> <Port>"); 
      
      //Convert host argument to InetAddres
      InetAddress serverHost = InetAddress.getByName(args[0]);
     
      //String serverHost = args[0];
      int port = Integer.parseInt(args[1]);
      
      //Create a datagram socket for receiving and sending UDP packets
      DatagramSocket socket = new DatagramSocket();
      socket.setSoTimeout(TIMEOUT);// receive() will only block for TIMEOUT in millis. Then SocketTimeoutExeption is raised
      
      int sequence_number = 0;
      int packets_lost = 0;
      int packets_received = 0;
      double packet_loss;
      long stime, etime;
      long RTT = 0;
      long totalRTT = 0;
      long minRTT = -1;
      long maxRTT = -1;
      double avgRTT = 0;
      
      
      //Processing loop.
      for(;sequence_number < NUM_PINGS; sequence_number++){
      
         //get time request is sent
         stime = System.currentTimeMillis();
         
         String message = "PING sequence_number=" + sequence_number + " time=" + stime + "\r\n";
         byte[] msg = message.getBytes(); //Message format PING <sequence_number> <time> CRLF
     
         //create a datagram packet to send request message
         DatagramPacket request = new DatagramPacket(msg, msg.length, serverHost, port);
         
         //Create Datagram to receive reply
         DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
         
         //send datagram request to host
         socket.send(request);
              
         //Block until the client receives a UDP packet.
         try{
            socket.receive(reply);
         }catch (SocketTimeoutException e){
            System.out.println("No reply for ping " + sequence_number + " was received.");
            packets_lost += 1;
            continue;
         }
         
         etime = System.currentTimeMillis(); //Get time of reply
         RTT = etime-stime; // Calculate round-trip time
         
         InetAddress servHost = reply.getAddress();
         int servPort = reply.getPort();
         //byte[] buf = reply.getData();
         
         if(minRTT > RTT) 
         {
            minRTT = RTT; // set min
         } else if (maxRTT < RTT){
            maxRTT = RTT; // set max
         } else if (minRTT == -1){ //if first run
       
            maxRTT = minRTT = RTT; // set min and max  
         }
      
         // add to total RTT;
         totalRTT += RTT;
        
         //Print the received data.
         printData(reply);
         
         //Print RTT
         System.out.println(" RTT=" + RTT + "ms");
         
         //Wait 1 minute between PING
         Thread.sleep(SEND_INTERVAL);
         
      }
      
      packets_received = NUM_PINGS - packets_lost;
      
      // Calculate average
      if(packets_lost != NUM_PINGS){ //avoid divide by zero
         avgRTT = (double)totalRTT/(packets_received);
      }
      
      
      //calculate packet loss
      packet_loss = ((double)packets_lost/NUM_PINGS)*100;
      
      //print statistics
      System.out.println("\n---- " + serverHost + " ping statistics----");
      System.out.format("%d packets transmitted, %d packets received, %.1f%% packet loss\n", NUM_PINGS, packets_received, packet_loss);
      System.out.format("round-trip min/avg/max = %d/%.3f/%d ms\n", minRTT, avgRTT, maxRTT );
      
      socket.close();
   }
   
   
   /*
    * Print ping data to the standard output stream
    */  
   private static void printData(DatagramPacket request) throws Exception
   {
      //Obtain references to the packet's array of bytes
      byte[] buf = request.getData();
      
      
      //Wrap the bytes in a byte array input stream,
      // so that you can read data as a stream of bytes.
      ByteArrayInputStream bais = new ByteArrayInputStream(buf);
      
      //Wrap the bytes in a byte array input stream reader,
      // so that you can read the data as a stream of characters
      InputStreamReader isr = new InputStreamReader(bais);
      
      //Wrap the input stream reader in a buffered reader,
      //so you can read the characer data a line at a time.
      //(A line is a sequence of chars terminated by any combination of \r and \n.)
      BufferedReader br = new BufferedReader(isr);
      
      //The message data is contained in a single line, so read this line.
      String line = br.readLine();
      
      //Pring host address and data received from it.
      System.out.print(
         "Reply from " + request.getAddress().getHostAddress() +
         ": " +
         new String(line) );
 
   }

}  
   