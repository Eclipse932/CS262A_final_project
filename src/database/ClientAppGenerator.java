package database;
import java.io.*;
import java.lang.*;
import java.util.Scanner;
public class ClientAppGenerator {

	public static void generateTrans(String[] args) throws IOException{
		Writer writer = null;
		int deadTime, conflict, length, totalVariable, totalTransaction;
	    deadTime = Integer.parseInt(args[0]);
	    conflict = Integer.parseInt(args[1]);
	    length = Integer.parseInt(args[2]);
	    totalVariable = Integer.parseInt(args[3]);
	    totalTransaction = Integer.parseInt(args[4]);
	    System.out.println("deadTime: " + deadTime + " conflict: " + conflict + " length: " + length + " totalVariable: " + totalVariable);
	    
	    double temp = 1.0;
		double readRatio = 0.0;
		double writeRatio = 0.0;
		double addRatio = 0.0;
		double addcRatio = 0.0;
		double waitRatio = 0.0;
		int client1Low, client1High, client2Low, client2High, client3Low, client3High; 

		int transNum = 0;
		while(transNum <= 4){
			if(transNum == 4){
				waitRatio = temp;
				temp -= temp;
				transNum ++;
			}else{
				double a = Math.round(Math.random()*temp*1000000.0)/1000000.0;
				while(a>0.35){
					a= Math.round(Math.random()*temp*1000000.0)/1000000.0;
				}
//						System.out.print(a +"\t");
				if(transNum == 0){
					readRatio = a;
				}else if(transNum == 1){
					writeRatio = a;
				}else if(transNum == 2){
					addRatio = a;
				}else{
					addcRatio = a;
				}
				temp -= a;
				transNum++;
			}
		}
		System.out.println("readRatio: " + readRatio + " writeRatio: " +writeRatio+ " addRatio: " +
				addRatio+ " addcRatio: " +addcRatio+ " waitRatio: "+waitRatio+"\n\n");
		
		if((readRatio+writeRatio+addRatio+addcRatio+waitRatio) != 1.0){
			System.out.println("total:" + (readRatio+writeRatio+addRatio+addcRatio+waitRatio));
			
		}
		
		
	
		client1Low = ((Double)(Math.random()*Integer.MAX_VALUE)).intValue() - 300;
		client1High = client1Low + 200;
		System.out.println((conflict*1.0)/100*client1High);
		client2Low = client1High - (int)((conflict*1.0)/100*200);
		client2High = client2Low+200;
		client3High = client1Low + (int)((conflict*1.0)/100*200);
		client3Low = client3High-200;
		System.out.println("The memory are: "+client1Low+'\t'+client1High+'\t'+client2Low+'\t'+client2High+'\t'+client3Low+'\t'+client3High);
		
		for(int i = 1; i<=3 ;i++){
			try {

				String storeFile = "test-file/client"+i+"/filename.txt";
				System.out.println(storeFile);
			    writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(storeFile), "utf-8"));
			    
			    //writer.write("The transaction is the following: \n");
			    
			    //how many transaction left
			    int transactionLeft = totalTransaction;
			    while(transactionLeft > 0){
			    
				    int lengthIn = length; 
					int readTotal = ((Double)(lengthIn*readRatio)).intValue();
					int writeTotal = ((Double)(lengthIn*writeRatio)).intValue();
					int addTotal = ((Double)(lengthIn*addRatio)).intValue();
					int addcTotal = ((Double)(lengthIn*addcRatio)).intValue();
					int waitTotal = ((Double)(lengthIn*waitRatio)).intValue(); 
				    /*
				     *  read <variable name> <memory address to be read from>
					 *  write <variable name> <memory address to be written to>
					 *  add <variable name sum> <variable name addend> <variable name addend>
					 *  addc <variable name sum> <variable name> <integer constant addend>
				  	 *  wait <integer time to sleep in milliseconds>
				     */
				    while(readTotal>0 || writeTotal>0 || addTotal>0 || addcTotal>0 || waitTotal>0){
				    	System.out.println(readTotal+ "\t" + writeTotal +"\t" + addTotal 
				    			+ "\t" + addcTotal +"\t" + waitTotal);
				    	int randomNumber = ((Double)(Math.random()*4.0)).intValue();
				    	switch(randomNumber){
				    		case 0:	
				    			if(readTotal>0){
				    				writer.write("read x"+ variableSelector(totalVariable) + " "+ 
				    								(i==1?memorySelector(client1Low, client1High):
				    								(i==2?memorySelector(client2Low, client2High):
				    								memorySelector(client3Low, client3High)))
				    								+"\n");
				    				readTotal--;
				    				lengthIn--;
				    			}
				    				
				    		case 1: 
				    			if(writeTotal>0){
				    				writer.write("write x"+ variableSelector(totalVariable) + " "+ 
		    								(i==1?memorySelector(client1Low, client1High):
		    								(i==2?memorySelector(client2Low, client2High):
		    								memorySelector(client3Low, client3High)))+
		    								"\n");
				    				writeTotal--;
				    				lengthIn--;
				    			}
				    				
				    		case 2: 
				    			if(addTotal>0){
				    				writer.write("add x"+ variableSelector(totalVariable) + " x"+variableSelector(totalVariable)+
				    						" x"+variableSelector(totalVariable)+"\n");
				    				addTotal--;
				    				lengthIn--;
				    			}
		    						
				    		case 3: 
				    			if(addcTotal>0){
				    				writer.write("addc x"+ variableSelector(totalVariable) + " x"+variableSelector(totalVariable) + " "+ 
				    						(int)Math.ceil(Math.random()*10000)+" \n");
				    				addcTotal--;
				    				lengthIn--;
				    			}
		    						
				    		case 4: 
				    			if(waitTotal>0){
				    				writer.write("wait "+ deadTime+ " \n");
				    				waitTotal--;		
				    				lengthIn--;
				    			}
				    	}
				    }
				    writer.write("ENDING TRANSACTION\n");
				    transactionLeft--;
			    }	
			} catch (IOException ex) {
			  // report
			} finally {
				writer.write("FILE END");
			   try {writer.close();} catch (Exception ex) {}
			}
		}
	}
	public static int variableSelector(int totalVariable){
		return (int)Math.ceil(Math.random()*totalVariable);
	}
	public static int memorySelector(int low, int high){
		
		return (int)Math.ceil(Math.random()*(high-low)+low);
	}
	public static void main(String[]args) throws IOException{
		generateTrans(args);
		
	}
	
}
