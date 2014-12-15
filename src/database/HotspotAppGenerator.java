package database;
import java.io.*;
import java.lang.*;
import java.util.Scanner;
public class HotspotAppGenerator {
	protected static int totalClients, totalTransaction;
	public static void generateTrans() throws IOException{
		Writer writer = null;
		int memoryLocation = 65397451;
		for(int i = 0; i<= totalClients; i++){
			try {
				String storeFile = "../hotspot-test-file/client"+i+"/filename.txt";
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storeFile), "utf-8"));
				if( i==0){
					System.out.println(storeFile);
					writer.write("declare x"+ i + " "+ (int)Math.ceil(Math.random()*10000) +"\n");
					writer.write("write x"+ i + " "+ memoryLocation + "\n");
					writer.write("ENDING TRANSACTION\n");
				}else{
					for(int j = 0; j< totalTransaction; j++){
						System.out.println(storeFile);
						writer.write("read x"+ j + " "+ memoryLocation +"\n");
						writer.write("ENDING TRANSACTION\n");
					}
				}
			}finally {
				try {writer.close();} catch (Exception ex) {}
			}
		}
	}
	public static void main(String[]args) throws IOException{
		totalClients = Integer.parseInt(args[0]);
		totalTransaction = Integer.parseInt(args[1]);
		generateTrans();
	}
}
