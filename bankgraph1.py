import datetime
import sys
import matplotlib.pyplot as plt
import numpy as np

earliestTime = 0
latestTime = 0
totalClient= sys.argv[1]
yMeanplot = []
yMinplot = []
yMaxplot = []
yCommitplot = []
yTotalplot = []
diffValue = []
xplot = []
clientNumber = []
clientHasCommit = False
for y in xrange(10, int(totalClient)+1 , 10):
  print y
  endingTransaction = False
  clientNumber.append('client'+str(y));
  totalCommit = 0
  totalTransaction = 0
  del diffValue[:]
  for x in range(1,y+1):
    fileName = "result/ClientApp/" + str(y) + "/3DclientAppLog"+ str(x) + ".txt" 	
    clientHasCommit = False
    with open(fileName) as fp:
      writeStatus = False
      for line in fp:
        if line == "Beginning Transaction\n" :
	  earliestTime = 0
          latestTime = 0
          writeStatus = False          
        if(line [:6] == "Return"):
          totalTransaction += 1     
	  if(line.split('\t')[1] == 'commit\n'):
            totalCommit += 1;
            writeStatus = True
        if(line[:4] == '2014'):
	  tempTime = (line.split('T')[1]).split('Z')[0]
        if writeStatus == True and tempTime != '' :
          clientHasCommit = True;
          print tempTime
	  if earliestTime == 0 or latestTime == 0 :
	  	earliestTime = tempTime
		latestTime = tempTime
                tempTime = ''
          else:
		if(float(tempTime.split(':')[0]) <= float(earliestTime.split(':')[0])):
		  if(float(tempTime.split(':')[1]) <= float(earliestTime.split(':')[1])):
		    if(float(tempTime.split(':')[2]) < float(earliestTime.split(':')[2])):
		      earliestTime = tempTime
 	        if(float(tempTime.split(':')[0]) >= float(latestTime.split(':')[0])):
		  latestTime = tempTime
		else:
		  if (float(tempTime.split(':')[1]) >= float(latestTime.split(':')[1])):
                    latestTime = tempTime
                  else:
		    if (float(tempTime.split(':')[2]) > float(latestTime.split(':')[2])):
		      print 'inside'
		      latestTime = tempTime
                tempTime = ''
        if endingTransaction == True:
          diff = (float(latestTime.split(':')[0]) -float(earliestTime.split(':')[0]))*3600 + (float(latestTime.split(':')[1]) - float(earliestTime.split(':')[1]))*60 
	  diff += (float(latestTime.split(':')[2]) - float(earliestTime.split(':')[2]))
          print latestTime + " " + earliestTime
          diffValue.append(diff)
          endingTransaction = False
        if line == "Ending Transaction\n" and writeStatus == True:
      	  tempTime = ''          
          print 'see ending transaction'
	  endingTransaction = True


    #if clientHasCommit == True:
    #  diff = (float(latestTime.split(':')[0]) -float(earliestTime.split(':')[0]))*3600 + (float(latestTime.split(':')[1]) - float(earliestTime.split(':')[1]))*60 
    #  diff = (float(latestTime.split(':')[2]) - float(earliestTime.split(':')[2]))
    #  diffValue.append( diff)   
  print diffValue
  yMinplot.append(min(diffValue))
  yMeanplot.append(reduce(lambda x, y: x + y, diffValue) / float(totalCommit))       
  yMaxplot.append(max(diffValue))  
  yCommitplot.append(totalCommit*100.0/totalTransaction)
  yTotalplot.append(totalTransaction)

# evenly sampled time at 200ms intervals
t = np.arange(10., (int(totalClient)+1), 10)
f= plt.figure(1)
# red dashes, blue squares and green triangles
#plt.plot(t, yMinplot, 'r--',label="min", t, yMaxplot, 'b--',label="Max", t, yMeanplot, 'g--',label="Mean")
line1, = plt.plot(t, yMinplot, label="min", linestyle='--')
line2, = plt.plot(t, yMaxplot, label="max")
line3, = plt.plot(t, yMeanplot, label="mean", linewidth=4)
# add some text for labels, title and axes ticks
plt.ylabel('time(s)')
plt.xlabel('Total Number of Clients')
plt.title('Runtime Comparison')
#plt.xticks(ind+width)
#plt.xticklabels(clientNumber)
# Place a legend to the right of this smaller figure.
#first_legend = plt.legend(handles=[line1], loc=1)
# Add the legend manually to the current Axes.
#ax = plt.gca().add_artist(first_legend)
#second_legend = plt.legend(handles=[line2], loc=2)
#third_legend = plt.legend(handles=[line3], loc=3)
#plt.legend(handler_map={line1: HandlerLine2D(numpoints=4)})
plt.legend([line1, line2, line3],["min" , "max", "mean"])
plt.show()

f= plt.figure(2)
line1, = plt.plot(t, yCommitplot, label="commit", linestyle='--')
# add some text for labels, title and axes ticks
plt.ylabel('Commit %')
plt.xlabel('Total Number of Clients')
plt.title('Commit Comparison')
plt.legend([line1],["% of commit"])
plt.show()


