package client;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

public class TestClient 
{
   public static void main(String[] args)
   {

      // create map to store
      Map<String, List<String>> map = new HashMap<String, List<String>>();
      // create list one and store values
      List<String> valSetOne = new ArrayList<String>();
      valSetOne.add("Apple");
      valSetOne.add("Aeroplane");
 
      // create list two and store values
      List<String> valSetTwo = new ArrayList<String>();
      valSetTwo.add("Bat");
      valSetTwo.add("Banana");
 
      // create list three and store values
	List<String> valSetThree = new ArrayList<String>();
	valSetThree.add("Cat");
	valSetThree.add("Car");
 
	// put values into map
	map.put("A", valSetOne);
	map.put("B", valSetTwo);
	map.put("C", valSetThree);

	System.out.println("Fetching Keys and corresponding [Multiple] Values n");
	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	String key = entry.getKey();
	List<String> values = entry.getValue();
	System.out.println("Key = " + key);
	System.out.println("Values = " + values + "n");
	}
      String host = "127.0.0.1";
      try 
      {
         Socket           client    = new Socket(host, 4321);
	
         //DataOutputStream socketOut = new DataOutputStream(client.getOutputStream());
         //DataInputStream  socketIn  = new DataInputStream(client.getInputStream());
         //DataInputStream  console   = new DataInputStream(System.in);
         ObjectOutputStream oout = new ObjectOutputStream(client.getOutputStream());
	 // write something in the file
         oout.writeObject(map);	  
	 // close the stream
         oout.close();
         System.out.println("Connected to " + host + ". Enter text:");
	 
         //socketOut.close(); socketIn.close(); 
         client.close();
      } 
      catch (UnknownHostException e) 
      { System.err.println(host + ": unknown host."); } 
      catch (IOException e) 
      { System.err.println("I/O error with " + host); }
   }
}

