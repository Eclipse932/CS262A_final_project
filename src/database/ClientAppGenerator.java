package database;
import java.io.*;
import java.lang.*;
import java.util.Scanner;
public class ClientAppGenerator {

	public static void generateTrans(String[] args) throws IOException{
		Writer writer = null;
		int deadTime, conflict,lowAddress, highAddress, length, totalVariable, totalTransaction, totalClient;
	    deadTime = Integer.parseInt(args[0]);
	    lowAddress = Integer.parseInt(args[1].split("-")[0]);
	    highAddress =  Integer.parseInt(args[1].split("-")[1]);
	    length = Integer.parseInt(args[2]);
	    totalVariable = Integer.parseInt(args[3]);
	    totalTransaction = Integer.parseInt(args[4]);
	    totalClient = Integer.parseInt(args[5]);
	    System.out.println("deadTime: " + deadTime + " low address: " + lowAddress 
	    		+" high address: "+highAddress+ " length: " + length + 
	    		"totalTransaction " + totalTransaction + " totalVariable: " + totalVariable);
	    
	    double temp = 1.0;
		double readRatio = 0.0;
		double writeRatio = 0.0;
		double addRatio = 0.0;
		double addcRatio = 0.0;
		double waitRatio = 0.0;

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

		for(int i = 1; i<=totalClient ;i++){
			try {
				
				String storeFile = "../test-file/client"+i+"/filename.txt";
				System.out.println(storeFile);
			    writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(storeFile), "utf-8"));
			    
			    //writer.write("The transaction is the following: \n");
			    
			    //how many transaction left
			    int transactionLeft = totalTransaction;
			    while(transactionLeft > 0){
			    	
				    int lengthIn = length; 

				    for(int varDeclare =1 ; varDeclare <= totalVariable ;varDeclare++){
				    	writer.write("declare x"+varDeclare + " "+ (int)Math.ceil(Math.random()*10000) +"\n");
				    }
				    
					while(lengthIn >= 0){
						double randomNum = Math.random();
						if( randomNum >= 0 && randomNum <= readRatio){
							writer.write("read x"+ variableSelector(totalVariable) + " "+ 
									memorySelector(lowAddress, highAddress) +"\n");
							lengthIn--;
						}else if(randomNum > readRatio && randomNum <= (readRatio+writeRatio) ){
		    				writer.write("write x"+ variableSelector(totalVariable) + " "+ 
		    						memorySelector(lowAddress, highAddress)+ "\n");
		    				lengthIn--;
						}else if(randomNum >(readRatio+writeRatio) && randomNum <=(readRatio+writeRatio+addRatio)){
							writer.write("add x"+ variableSelector(totalVariable) + " x"+variableSelector(totalVariable)+
		    						" x"+variableSelector(totalVariable)+"\n");
		    				lengthIn--;
						}else if(randomNum > (readRatio+writeRatio+addRatio) && randomNum <= (readRatio+writeRatio+addRatio+addcRatio)){
		    				writer.write("addc x"+ variableSelector(totalVariable) + " x"+variableSelector(totalVariable) + " "+ 
		    						(int)Math.ceil(Math.random()*10000)+"\n");
		    				lengthIn--;
						}else{
		    				writer.write("wait "+ deadTime+ "\n");
		    				lengthIn--;
						}
					}
				    writer.write("ENDING TRANSACTION\n");
				    transactionLeft--;
			    }	
			} catch (IOException ex) {
			  // report
			} finally {
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
