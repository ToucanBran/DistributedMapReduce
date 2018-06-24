# DistributedMapReduce

Final project for CSC-536

*********Overview & Architecture*********

This application utilizes the Akka framework and Scala to create an akka cluster using the 
mapreduce code from a previous assignment. 
Each node runs in its own docker container. Upon joining the cluster, every node 
loads up the a clustersingletonmanager / clustersingletonproxy object so that 
there is only one service that receives and forwards jobs to map actors. All nodes 
create 4 map actors and 4 reduce actors, putting them into a cluster aware router so 
that the master MapReduceService can utilize them. This means if there are 4 nodes, 
16 map actors are put into the pool. 

There are two distinct containers running (other than the bootstrap service): 
CloudClient and MapReduce. There will only be one CloudClient joining the cluster 
and as it does, it waits until there's at least one other node involved before sending 
jobs to it. As the MapReduce nodes join(see cloudmasteractor and mapreduceservice), jobs 
are forwarded via the proxy which routes to the MapReduceService object. The 
MapReduceService receives messages and forwards them on based on the case. 

*********How to run*********

I've uploaded both the CloudClient and MapReduce images to my docker repo so there's no need 
to run 'sbt docker:publishLocal'. All you have to do is navigate to the DistributedMapReduce 
folder that contains the docker-compose.yml file and run the command: 'docker-compose up'. 
You'll start seeing log statements print to the screen. Make sure you have a docker machine running
first and you're hooked into it after running the command generated from docker-machine env {vm_name}.

********Odd Behavior and Things I've learned*********

This final project proved to be especially difficult, given the lack of information on the internet 
at setting something up like this. I noticed a few odd behaviors that you'll likely see when running:

========Slow Performance========
Running the original mapreduce code performs EXTREMELY fast compared to running it in the cluster. 
I know this is most likely a configuration error on my part but what I think happens is the cluster 
doesn't seem to be stable a lot of the time and nodes go in and out of their up status. This means that 
it takes jobs a while to be forwarded or mapped or reduced. I researched for days on how to prevent this but was unsuccessful.

========Undetected Nodes========
The CloudClient will not send jobs unless there is at least one other node in the cluster. Sometimes it never detects
a node or takes a really long time before it sees it. This is an intermittant error that I was unable to figure out.

====More Nodes,More Problems====
When I use docker-compose --scale node3= x where x is some number to replicate this specific node, even more odd 
behavior occurs such as the client sending the jobs and then starting itself back up again. 

====If I could do it over again====
I'd try to just create a cluster of only Mappers and Reducers which receive jobs from the client 
via ClusterClient and ClusterClientReceptionist. There was also another good example I saw where they had 
a ClusterSingletonManager that received jobs and put them into a persistent queue. When workers were ready 
for work, they'd notify the Manager who would then forward them the job. This was also one I really liked but 
I didn't think I'd have the time to implement it.
