import java.awt.Color;
import java.net.*;
import java.util.*; 
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Lohith
 */
public class NetworkListener  extends Thread
{
    int listenport;
    DatagramSocket clientSocket;
    Main guiinst;
    String ID;
    String pwd;
    int serverport;

    InetAddress serveripaddress;
    
    
    

    

    NetworkListener(int lp,Main g,String id , String proxyip , int port)
    {
       listenport = lp;
       guiinst = g;
       ID = id;
       serverport = port;
      
       


       try
       {
          serveripaddress = InetAddress.getByName(proxyip);
          
       }
       catch(Exception e)
       {
           e.printStackTrace();
       }



    }


    void sendMessage(String msg)
    {

        System.out.println("sent msg:" + msg);
        
        guiinst.writetolog("Sent:" + msg);
        try
        {
            byte[] sendData = new byte[1500];

            //for(int i= 0; i < sendData.length; i++)
            //{
            //   sendData[i] = 0;
            //}

            sendData = msg.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                 serveripaddress, serverport);

            clientSocket.send(sendPacket);


        }
        catch(Exception e)
        {

        }


    }
   
    void sendRegistartion()
    {

        
        try
        {
            String reg = "REGISTERUSER#" +ID + "#" + pwd + "#END";


            byte[] sendData = new byte[500];

            //for(int i= 0; i < sendData.length; i++)
            //{
            //   sendData[i] = 0;
            //}

            sendData = reg.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                 serveripaddress, serverport);

            clientSocket.send(sendPacket);


        }
        catch(Exception e)
        {
          e.printStackTrace();

        }
        


    }

    

   

  

      


    public void run()
    {
        try
        {
            

                       
            clientSocket = new DatagramSocket(listenport);
            byte[] receiveData = new byte[500];

           

            sendRegistartion();
            
            while(true)
            {



                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                  clientSocket.receive(receivePacket);
                  String sentence = new String( receivePacket.getData());
                  //System.out.println("RECEIVED: " + sentence);
                  
                  guiinst.writetolog("RECEIVED data " );

                  if (sentence.startsWith("REGISTERUSERACK#"))
                  {
                     
                      
                      InetAddress IPAddress = receivePacket.getAddress();
                      int port = receivePacket.getPort();

                      guiinst.writetolog(" login succssfully to Charm Manager");

                      guiinst.handleLoginSuccess();
                      
                  }
                  if (sentence.startsWith("REGISTERUSERFAILED#"))
                  {
                      guiinst.writetolog("Registration failed , invalid user, password");
                      
                  }
                  if (sentence.startsWith("FILESAVERES#"))
                  {

                     String [] parts = sentence.split("#");

                     guiinst.writetolog("Recieved File save res for " + parts[1] + " for totcost:" + parts[2]);

                     Calendar cd = Calendar.getInstance();
                     Date dt = cd.getTime();

                     String ct = dt.toString() + "," + parts[1] + ",storage," + parts[2];
                     FileAppender.AppendtoFile("report.txt", ct);
                     // String resmsg = "FILESAVERES#" + filename + "#" + totcost + "#END";

                  }

                  if (sentence.startsWith("FILEDOWNLOADRES#"))
                  {

                       String [] parts = sentence.split("#");

                     // String fres = "FILEDOWNLOADRES#" + parts[1] + "#" + parts[2] + "#" + s + "#END";
                     guiinst.writetolog("Recieved File downlod res for " + parts[1] + " for totcost:" + parts[2]);

                     guiinst.writetolog("File content:" + parts[3]);

                     Calendar cd = Calendar.getInstance();
                     Date dt = cd.getTime();

                     String ct = dt.toString() + "," + parts[1] + ",Access," + parts[2];
                     FileAppender.AppendtoFile("report.txt", ct);



                  }





                  

               
                  
                 


            }


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }



    }



}
