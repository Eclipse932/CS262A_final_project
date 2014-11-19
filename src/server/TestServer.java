package server;
import java.net.*;
import java.io.*;
import java.util.*;

public class TestServer
{
   public static void main(String args[])
   {
      ServerSocket server = null;
      try 
      {
         server = new ServerSocket(4321);
      } 
      catch (IOException e) 
      {
         System.out.println("Error on port: 4321 " + ", " + e);
         System.exit(1);
      }

      System.out.println("Server setup and waiting for client connection ...");

      Socket client = null;
      try 
      {
         client = server.accept();
      } 
      catch (IOException e) 
      {
         System.out.println("Did not accept connection: " + e);
         System.exit(1);
      }

      System.out.println("Client connection accepted. Moving to local port ...");

      try
      {
	/*
         DataInputStream streamIn = new 
                  DataInputStream(new 
                  BufferedInputStream(client.getInputStream()));

         boolean done = false;
         String line;
         while (!done)
         {
            line = streamIn.readLine();
            if (line.equalsIgnoreCase(".bye"))
               done = true;
            else
               System.out.println("Client says: " + line);
         }

         streamIn.close();
         client.close();
         server.close();*/

	ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
	HashMap map = (HashMap)  ois.readObject();
         System.out.println(map.get("A"));
         
      
	}
      catch(IOException e)
      { 
           System.out.println("IO Error in streams " + e); 
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
}
