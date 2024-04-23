
import java.net.*;
import java.sql.ResultSet;
import java.util.*; 
import org.logi.crypto.Crypto;
import org.logi.crypto.secretshare.PolySecretShare;
import org.logi.crypto.secretshare.SecretShare;

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
    DatagramSocket serverSocket;
    StorageManager guiinst;

    

    HashMap  clientDetails  = new HashMap();

    HashMap requestforfile = new HashMap();


    NetworkListener(int lp,StorageManager g)
    {
       listenport = lp;
       guiinst = g;

    }

   

    void sendMsg(String msg,InetAddress ip,int port)
    {
        System.out.println("Sent " + msg);
        
        guiinst.writetolog("Sent :" + msg);
         byte[] sendData = new byte[1000];

         for(int i =0 ;i < sendData.length;i++)
         {
             sendData[i] = 0;
         }

        sendData = msg.getBytes();

        try
        {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            serverSocket.send(sendPacket);
        }
        catch(Exception e)
        {
            System.out.println("Send failed");

        }



    }


    void sendShareStorageRequest(CloudClient cl,String filename,String msg)
    {

        String msgcont = "SAVEBLOCK#" + filename + "#" + msg + "#";

        byte[] sendData = new byte[1000];

         for(int i =0 ;i < sendData.length;i++)
         {
             sendData[i] = 0;
         }

        sendData = msgcont.getBytes();

        try
        {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cl.IPAddress, cl.port);
            serverSocket.send(sendPacket);
        }
        catch(Exception e)
        {
            System.out.println("Send failed");

        }

        


    }

    void sendRegsiterResponse(String id)
    {
        CloudClient ps = (CloudClient)clientDetails.get(id);

        if (ps == null)
        {
           System.out.println("client  not found");
           return;

        }

        
        
        String query = "REGISTERACK";
        
        byte[] sendData = new byte[500];

         for(int i =0 ;i < sendData.length;i++)
         {
             sendData[i] = 0;
         }

        sendData = query.getBytes();

        try
        {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ps.IPAddress, ps.port);
            serverSocket.send(sendPacket);
        }
        catch(Exception e)
        {
            System.out.println("Send failed");

        }


    }

    void handleFileSave(String sent,InetAddress cliip,int clport)
    {

         //String msg = "FILETOSAVE#" + loginname.getText() + "#" + filenamealone + "#" + cont + "#END";

        String [] parts = sent.split("#");
        String u = parts[1];
        String filename=parts[2];
        String filcon = parts[3];

        guiinst.writetolog("Recieved file save request from " + u + " for filename" + filename);

        try
        {

             Database db = new Database();

             Crypto.initRandom();

             SecretShare[] shares = null;

             shares = PolySecretShare.share(1,2,filcon.getBytes(),100);

             for (int i=0; i<shares.length; i++) {

                guiinst.writetolog(" Share " + (i+1));
                guiinst.writetolog(shares[i].toString());
             }

             // storage scheduling.
            // sort the clouds based on storage cost
            Vector<CloudClient> allcli = new Vector<CloudClient>();

            Collection c = clientDetails.values();

            Iterator it = c.iterator();

            while (it.hasNext())
            {
                CloudClient ps = (CloudClient)it.next();
                allcli.add(ps);

            }
            Collections.sort(allcli,new StorageCostSorter());

            guiinst.writetolog("Sorted Cloud Clients based on storage cost");

            for (int i=0;i<allcli.size();i++)
            {
                guiinst.writetolog(allcli.get(i).ID + "==>" + allcli.get(i).scost);

            }

            int pos=0;

            String extensionRemoved = filename.split("\\.")[0];

            int totcost=0;

            String cloudidallocated=null;

            for (int i=0; i<shares.length; i++) {

                guiinst.writetolog("Sending Share " + (i+1) + " to " + allcli.get(pos).ID);

                if (cloudidallocated==null)
                {
                   cloudidallocated = allcli.get(pos).ID;
                }
                else
                {
                   cloudidallocated = cloudidallocated + "," + allcli.get(pos).ID;

                }

                guiinst.writetolog("Total bytes sent:" + shares[i].toString().length());
                guiinst.writetolog("Cost for it:" + shares[i].toString().length() * allcli.get(pos).scost);
                
                totcost = totcost + shares[i].toString().length() * allcli.get(pos).scost;



                String fname = extensionRemoved + "_" + (i+1);

                sendShareStorageRequest(allcli.get(pos),filename,shares[i].toString());

                pos = (pos+1)%allcli.size();


            }

            String query = "insert into filehistory values('" +  filename + "','" + cloudidallocated + "')";

            db.executeUpdate(query);

            db.close();
            

            guiinst.writetolog("Total cost for storage is " + totcost);

            String resmsg = "FILESAVERES#" + filename + "#" + totcost + "#END";

            sendMsg(resmsg,cliip,clport);






        }
        catch(Exception e)
        {
             e.printStackTrace();

        }





        
        
    }

    void sendFileDownloadReq(CloudClient cl,String filename)
    {
        guiinst.writetolog("Sending filedownload for file:" + filename + " to cloud " + cl.ID);
        String msg = "GETBLOCK#" + filename + "#END";

        byte[] sendData = new byte[500];

        for(int i =0 ;i < sendData.length;i++)
        {
             sendData[i] = 0;
        }

        sendData = msg.getBytes();

        try
        {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cl.IPAddress, cl.port);
            serverSocket.send(sendPacket);
        }
        catch(Exception e)
        {
            System.out.println("Send failed");

        }
        


    }
    void handleFileDownload(String sent,InetAddress dipadd,int dport)
    {

         //String msg = "FILETOGET#" + loginname.getText() + "#" + filetodownload.getText() + "#END";

         String [] parts = sent.split("#");
         
         guiinst.writetolog("Recieved file get request from " + parts[1] + " for file " + parts[2]);

         String query = "select cloudallocated  from filehistory where filename='" + parts[2] + "'";

         FileDownloadReq flreq = new FileDownloadReq();
         flreq.filename = parts[2];
         flreq.username = parts[1];
         flreq.ipadd = dipadd;
         flreq.port = dport;
         requestforfile.put(flreq.filename,flreq);

         
         try
         {
             Database db = new Database();
             ResultSet rs = db.executeQuery(query);
             String clall=null;
             while (rs.next())
             {
                 clall =  rs.getString("cloudallocated");

             }

             if (clall!=null)
             {
                 String [] pa = clall.split(",");

                 CloudClient cl1 = (CloudClient)clientDetails.get(pa[0]);
                 CloudClient cl2 = (CloudClient)clientDetails.get(pa[1]);

                 if (cl1.acost<cl2.acost)
                 {
                     
                     sendFileDownloadReq(cl1,parts[2]);
                 }
                 else
                 {
                     sendFileDownloadReq(cl2,parts[2]);
                 }

             }

           
         }
         catch(Exception e)
         {
             
         }

         






    }


    void handleBlockRes(String senten)
    {

        //String msgres = "BLOCKRES#" + filename + "#" + cost + "#" + cont + "#END";

        String [] parts = senten.split("#");
        FileDownloadReq flreq = (FileDownloadReq)requestforfile.get(parts[1]);

        try
        {
            if (flreq!=null)
            {
                SecretShare[] shares = new SecretShare[1];

                shares[0] = PolySecretShare.parseCDS(parts[3].split(","));

                byte [] orig;

                orig = PolySecretShare.retrieve(shares);
                String s = new String(orig);
                System.out.println("The File content retrieved from shares is " + s);
                                
                String fres = "FILEDOWNLOADRES#" + parts[1] + "#" + parts[2] + "#" + s + "#END";

                sendMsg(fres,flreq.ipadd,flreq.port);
                



            }
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

            serverSocket = new DatagramSocket(listenport);
            byte[] receiveData = new byte[1024];
            


            guiinst.writetolog("Storage Manager started..  waiting for messages");
            
            while(true)
            {
                 

                  for(int i =0; i < receiveData.length; i++)
                  {
                       receiveData[i] = 0;

                  }

                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                  serverSocket.receive(receivePacket);
                  String sentence = new String( receivePacket.getData());
                  System.out.println("RECEIVED: " + sentence);

                  //guiinst.writetolog("Received: " + sentence);
                  InetAddress IPAddress = receivePacket.getAddress();
                  int port = receivePacket.getPort();

                  if (sentence.startsWith("REGISTER#"))
                  {
                      guiinst.writetolog("Received: REGISTER ");

                      

                      CloudClient ps = new CloudClient();
                      ps.IPAddress = IPAddress;
                      ps.port = port;

                      String []temp = sentence.split("#");

                      ps.ID = temp[1];

                      ps.scost = Integer.parseInt(temp[2]);

                      ps.acost = Integer.parseInt(temp[3]);
                      
                      clientDetails.put(ps.ID, ps);

                      guiinst.displayRegisteredUnits(clientDetails);

                      sendRegsiterResponse(ps.ID);



                  }
                  if (sentence.startsWith("REGISTERUSER#"))
                  {
                      guiinst.writetolog("recieved " + sentence);
                      String [] parts = sentence.split("#");
                      
                      //String reg = "REGISTERUSER#" +ID + "#" + pwd + "#END";
                      String u = parts[1];
                      String p = parts[2];
                      
                      try
                      {
                          Database db = new Database();
                          String q = "select * from login where username='" + u + "' and password='" + p + "'";
                          ResultSet rs =db.executeQuery(q);
                          if (rs.next())
                          {
                              String resmsg = "REGISTERUSERACK#END";
     
                              sendMsg(resmsg,IPAddress,port);
                              
                              
                          }
                          else
                          {
                              String resmsg = "REGISTERUSERFAILED#END";
                              
                              sendMsg(resmsg,IPAddress,port);
                              
                              
                          }
                         
                          
                      }
                      catch(Exception ex)
                      {
                          
                      }
                      
                      
                      
                  }

                  if (sentence.startsWith("FILETOSAVE#"))
                  {
                      //String msg = "FILETOSAVE#" + loginname.getText() + "#" + filenamealone + "#" + cont + "#END";

                      handleFileSave(sentence,IPAddress,port);


                  }
                  if (sentence.startsWith("FILETOGET"))
                  {
                      //String msg = "FILETOGET#" + loginname.getText() + "#" + filetodownload.getText() + "#END";

                      handleFileDownload(sentence,IPAddress,port);

                  }
                  if (sentence.startsWith("BLOCKRES#"))
                  {
                     
                      handleBlockRes(sentence);


                  }
                  if (sentence.startsWith("UDPATECOST#"))
                  {
                      String [] parts = sentence.split("#");
                      // String msg = "UDPATECOST#" + nwlisten.ID + "#" + scost + "#" + acost + "#";

                      CloudClient cl = (CloudClient)clientDetails.get(parts[1]);
                      if (cl!=null)
                      {
                          cl.scost = Integer.parseInt(parts[2]);
                          cl.acost = Integer.parseInt(parts[3]);

                      }




                  }
                  


            }


        }
        catch(Exception e)
        {
            e.printStackTrace();
        }



    }


    


}
