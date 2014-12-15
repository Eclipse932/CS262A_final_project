import datetime
import sys
import matplotlib.pyplot as plt
import numpy as np


totalClient= sys.argv[1]
yMeanplot = []
yMinplot = []
yMaxplot = []
yCommitplot = []
yTotalplot = []
diffValue = []
xplot = []
clientNumber = []
for y in range(1, int(totalClient)+1):
  clientNumber.append('client'+str(y));
  totalCommit = 0
  totalTransaction = 0
  del diffValue[:]
  for x in range(1,y+1):
    fileName = "result/ClientApp/" + str(y) + "/clientAppLog"+ str(x) + ".txt" 	
    with open(fileName) as fp:
      hasSeenEarliestTime = False
      hasSeenLatestTime = False
      earliestTime = None
      latestTime = None
      commit  = False
      earliestTime = None
      latestTime = None
      for line in fp:
        if line == "Beginning Transaction\n" :
          hasSeenEarliestTime = True
          hasSeenLatestTime = False
          earliestTime = None
          latestTime = None
          commit  = False
        elif line == "Ending Transaction\n":
          hasSeenLatestTime = True
        elif(line [:6] == "Return"):
          totalTransaction += 1     
	  if(line.split('\t')[1] == 'commit\n'):
            totalCommit += 1;
            commit = True
        elif (line[:4] == '2014') and hasSeenLatestTime == True:
          latestTime =  (line.split('T')[1]).split('Z')[0]
        elif(line[:4] == '2014') and hasSeenEarliestTime == True:
	        earliestTime =  (line.split('T')[1]).split('Z')[0]

        if earliestTime != None and latestTime != None and commit == True:
          diff = (float(latestTime.split(':')[0]) - float(earliestTime.split(':')[0]))*3600 + (float(latestTime.split(':')[1]) - float(earliestTime.split(':')[1]))*60 
	  diff += (float(latestTime.split(':')[2]) - float(earliestTime.split(':')[2]))
          print latestTime + " " + earliestTime
          diffValue.append(diff)
          commit = False
      
  print diffValue
  yMinplot.append(min(diffValue))
  yMeanplot.append(reduce(lambda x, y: x + y, diffValue) / float(totalCommit))       
  yMaxplot.append(max(diffValue))  
  yCommitplot.append(totalCommit*100.0/totalTransaction)
  yTotalplot.append(totalTransaction)

# evenly sampled time at 200ms intervals
t = np.arange(1., (int(totalClient)+1), 1)
f= plt.figure(1)
# red dashes, blue squares and green triangles
#plt.plot(t, yMinplot, 'r--',label="min", t, yMaxplot, 'b--',label="Max", t, yMeanplot, 'g--',label="Mean")
line1, = plt.plot(t, yMinplot, label="min", linestyle='--')
line2, = plt.plot(t, yMaxplot, label="max")
line3, = plt.plot(t, yMeanplot, label="mean", linewidth=4)
# add some text for labels, title and axes ticks
plt.ylabel('time(ms)')
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


