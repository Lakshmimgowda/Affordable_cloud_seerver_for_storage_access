import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    CloudClient guiinst;
    String ID;
    int serverport;

    InetAddress serveripaddress;
    int scost;
    int acost;
    
  

    
    

    

    NetworkListener(int lp,CloudClient g,String id , String proxyip , int port,int scost,int acost)
    {
       listenport = lp;
       guiinst = g;
       ID = id;
       serverport = port;
       this.scost = scost;
       this.acost = acost;
       
       


       try
       {
          serveripaddress = InetAddress.getByName(proxyip);
          
       }
       catch(Exception e)
       {
           e.printStackTrace();
       }



    }


    void sendMsg(String cont,InetAddress cipadd,int cport)
    {

         try
        {
            
            byte[] sendData = new byte[500];
            sendData = cont.getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                                 cipadd, cport);

            clientSocket.send(sendPacket);


        }
        catch(Exception e)
        {
          e.printStackTrace();

        }
        


    }
   
    void sendRegistartion()
    {

        try
        {
            String reg = "REGISTER#" +ID + "#" + scost + "#" + acost + "#END";

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

    

   

   void handleSaveBlock(String msg)
   {

       guiinst.writetolog("Recieved Save block request");

       String [] parts = msg.split("#");

       String name = parts[1];

       String con  = parts[2];

       FileAppender.AppendtoFile("./recvblocks" +ID+"/" + name, con);
       
       try
       {
           guiinst.s3interface.uploadFileToCloud("./recvblocks" +ID+"/" + name,name);
           
       }
       catch(Exception e)
       {
           e.printStackTrace();
       }

       guiinst.writetolog("Saved in blocks");


   }

    public static String readFileAsString(String filePath)
   {

        try
        {
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader = new BufferedReader(
            new FileReader(filePath));
            char[] buf = new char[1024];
            int numRead=0;
            while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
            }
            reader.close();
            return fileData.toString();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

   void handleGetBlock(String msg,InetAddress rip,int rport)
   {

      // String msg = "GETBLOCK#" + filename + "#END";

       String [] parts = msg.split("#");

       String filename = parts[1];

       guiinst.writetolog("Recieved get block request for " + filename);


        try
        {
            File folder = new File("./recvblocks" + ID);
            File[] listOfFiles = folder.listFiles();


            for (int i = 0; i < listOfFiles.length; i++) {


                if (listOfFiles[i].isFile()) {

                    String n = listOfFiles[i].getName();
                    if (n.equals(filename))
                    {
                        String cont =readFileAsString(listOfFiles[i].getCanonicalPath());

                        guiinst.writetolog(" the content length:" + cont.length());
                        
                        int totcost=cont.length()*acost;
                        
                        guiinst.writetolog("cost calculated is " + totcost);
                        
                        String msgres = "BLOCKRES#" + filename + "#" + totcost + "#" + cont + "#END";

                        guiinst.writetolog("Sending block res " + msgres + " to charm manager");

                        sendMsg(msgres,rip,rport);

                        break;

                    }

                }
             
            }
        }
        catch(Exception e)
        {

        }




   }


    public void run()
    {
        try
        {
            

                       
            clientSocket = new DatagramSocket(listenport);
            byte[] receiveData = new byte[500];

            guiinst.writetolog(" Sending registration message to Charm Manager");
            sendRegistartion();
            

            while(true)
            {



                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                  clientSocket.receive(receivePacket);
                  String sentence = new String( receivePacket.getData());
                  //System.out.println("RECEIVED: " + sentence);

                  InetAddress IPAddress = receivePacket.getAddress();
                  int port = receivePacket.getPort();

                  guiinst.writetolog("RECEIVED data "  + sentence);

                  if (sentence.startsWith("REGISTERACK"))
                  {
                     
                      
                      
                      guiinst.writetolog(" registered succssfully to Charm Manager");

                     
                      
                  }
                  if (sentence.startsWith("SAVEBLOCK#"))
                  {
                      handleSaveBlock(sentence);

                  }
                  if (sentence.startsWith("GETBLOCK#"))
                  {
                      //String msg = "GETBLOCK#" + filename + "#END";
                      handleGetBlock(sentence,IPAddress,port);

                      
                  }
                  
                 


            }


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }



    }



}
