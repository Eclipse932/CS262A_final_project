package database;
import java.io.*;
import java.lang.*;
import java.util.Scanner;

public class BankAppGenerator {
	protected static int totalClients, totalVariable, minActEachClient, length, totalTransaction;
	protected static int [][] memoryAdress;

	public static void generateTrans() throws IOException{
		Writer writer = null;
		
	    double temp = 1.0;
		double addMoneyRatio = 0.0;
		double transMoneyRatio = 0.0;

		int transNum = 0;
		while(transNum <= 1){
			if(transNum == 1){
				transMoneyRatio = temp;
				temp -= temp;
				transNum ++;
			}else{
				double a = Math.round(Math.random()*temp*1000000.0)/1000000.0;
				while(a<0.45 && a > 0.65){
					a= Math.round(Math.random()*temp*1000000.0)/1000000.0;
				}
//				System.out.print(a +"\t");
				if(transNum == 0){
					addMoneyRatio = a;
				}
				temp -= a;
				transNum++;
			}
		}
		
//		int [] ActEachClient = new int[totalClients];
//		for(int i = 1; i<=totalClients ;i++){
//			ActEachClient[i-1] = (int)Math.ceil(Math.random()*(1000)+minActEachClient);
//		}
		memoryAdress = new int [totalClients][totalVariable];
		for(int i = 1; i<=totalClients ;i++){
		    for(int varDeclare =1 ; varDeclare <= totalVariable ;varDeclare++){
		    	memoryAdress[i-1][varDeclare-1] = memoryAddressGenerator();
		    }
		}
		for(int i = 1; i<=totalClients ;i++){
			try{
				String storeFile = "../bank-test-file/client"+i+"/filename.txt";
			    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storeFile), "utf-8"));
			    for(int j = 0; j< totalTransaction ; j++){
				    for(int varDeclare =1 ; varDeclare <= totalVariable ;varDeclare++){
				    	writer.write("declare x"+varDeclare + " "+ (int)(Math.ceil(Math.random()*100000) +10000 ) +"\n");
				    	if(j==0)
				    		writer.write("write x"+ varDeclare + " "+ memoryAdress[i-1][varDeclare-1] + "\n");
				    }
			    	for( int k = 0 ; k< length ; k++){
			    		double randomNum = Math.random();
			    		int variableSelected;
			    		int variableTargetSelected;
			    		int memoryLocation;
			    		int moneyTransferred;
			    		int memoryTargetLocation;
						if( randomNum >= 0 && randomNum <= addMoneyRatio){
							variableSelected = variableSelector(totalVariable);
							memoryLocation = memorySelector(i-1);
							writer.write("read x"+ variableSelected + " "+ memoryLocation +"\n");
							writer.write("addc x"+ variableSelector(totalVariable) + " x"+variableSelector(totalVariable) + " "+ (int)Math.ceil(Math.random()*10000)+"\n");
					    	writer.write("write x"+ variableSelected + " "+ memoryLocation + "\n");

						}else {
							moneyTransferred = (int)(Math.ceil(Math.random()*100000) +10000 );
							//subtract money
							memoryLocation = memorySelector(i-1);
							variableSelected = variableSelector(totalVariable);
							writer.write("read x"+ variableSelected + " "+ memoryLocation +"\n");
							writer.write("addc x" + variableSelected + " x"+ variableSelected+ " " + (-1*moneyTransferred) + "\n");
					    	writer.write("write x"+ variableSelected + " "+ memoryLocation + "\n");
					    	//add money
					    	int transferMoneyTo;
					    	do{
					    		transferMoneyTo = (int)(Math.floor(Math.random()*totalClients) );
					    	}while(transferMoneyTo == i);
					    	
					    	memoryTargetLocation = memorySelector(transferMoneyTo);
							variableTargetSelected = variableSelector(totalVariable);
							writer.write("read x"+ variableTargetSelected + " "+ memoryTargetLocation +"\n");
		    				writer.write("addc x"+ variableTargetSelected + " x"+variableTargetSelected + " "+ moneyTransferred+ "\n");
					    	writer.write("write x"+ variableSelected + " "+ memoryTargetLocation + "\n");

						}
			    	}
				    writer.write("ENDING TRANSACTION\n");
			    }
			}finally {
				try {
					writer.close();
				} catch (Exception ex) {}
			}    
		}
	}
	
	public static int memoryAddressGenerator(){
		int max = totalClients*10000;
		int min = 1;
		int temp;
		do{
			temp = (int)(Math.ceil(Math.random()*max) +min );
		}while(!(isItSelected(temp)));
		return temp;
	}
	
	public static int memorySelector(int numClients){
		int y = (int)(Math.floor(Math.random()*totalVariable) );
		return memoryAdress[numClients][y];
	}
	
	public static boolean isItSelected(int mem){
		for( int i = 0 ; i< totalClients ; i++){
			for( int j = 0; j< totalVariable; j++){
				if(memoryAdress[i][j] == mem){
					return false;
				}
			}
		}
		return true;
	}
	public static int variableSelector(int totalVariable){
		return (int)Math.ceil(Math.random()*totalVariable);
	}
	public static void main(String[]args) throws IOException{
		totalClients = Integer.parseInt(args[0]);
		totalVariable = Integer.parseInt(args[1]);
		minActEachClient = Integer.parseInt(args[2]);
		length = Integer.parseInt(args[3]);
		totalTransaction = Integer.parseInt(args[4]);
		generateTrans();
	}
}
