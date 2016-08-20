package scheduler;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.ArrayList;

public class BranchAndBound {
	Schedule currentSchedule;
	Graph g;
	Node nodeToBeRemoved = null;
	public BranchAndBound(Schedule s, Graph g){
		this.currentSchedule = new Schedule(s.schedule, s.procLengths, s.scheduleLength);
		this.g = g;
	}
	/**
	 * The function that should initialise the branch and bound algorithm, While branch is giving a false (ie no better solution), loop the following:
	 * Take away last node from schedule, if this is the first node of the schedule, then check if any other root nodes that have not been processed as the first node in the schedule.
 	 * If all root nodes have being processed, then we have the optimal solution hopefully.
	 * @param schedule
	 * @param g
	 * @return
	 */
	public void branchAndBoundAlgorithm() {
		//make a list of root nodes
		ArrayList<Node> rootNodes = new ArrayList<Node>();
		ArrayList<Integer> rootNodeIDs = ScheduleHelper.findRootNodes(g);
		
		for(int i : rootNodeIDs){
			rootNodes.add(g.getNode(i));
		}
		
		nodeToBeRemoved = currentSchedule.schedule.get(currentSchedule.schedule.size() - 1);
		
		//Start the branch and bound
		while(Branch(new Schedule(currentSchedule.schedule, currentSchedule.procLengths, currentSchedule.scheduleLength)) == false){
			nodeToBeRemoved = currentSchedule.schedule.get(currentSchedule.schedule.size() - 1);
			currentSchedule.removeNode(currentSchedule.schedule.size() - 1);
			updateRemoveLengthChanges(currentSchedule, nodeToBeRemoved);
			
			if(currentSchedule.schedule.isEmpty()){
				//If schedule is empty, then what we removed is a root node
				//Remove it from the list of root nodes
				rootNodes.remove(nodeToBeRemoved);
				if(rootNodes.isEmpty()){
					//No more root nodes to process, the search is complete. the current best will be stored in ScheduleHelper
					return;
				}
				//Otherwise, put a new root node in and start branch and bound again. (This part will be different for the parallel version)
				currentSchedule.addNode(rootNodes.get(0), 0, 0);
			}
		}
	}
	
	/**
	 * Recursive function, find all processable nodes, for each processable children nodes, check how much the schedule increases when trying to add them to each of the processors,
	 * if the schedule time after adding to that processor is less than the current best schedule time (currentBestSchedule.scheduleLength) then insert node into the current schedule
	 * and recursively call Branch with the current schedule (this should be done for each of the nodes/processors that produce a lower schedule length)
	 * if the path is larger then return false, if no more nodes then return true.
	 * 
	 * @return
	 */
	public boolean Branch(Schedule branchingSchedule) {
		
		boolean hasInserted = false;

		for (Node n : g) {
			if (!branchingSchedule.schedule.contains(n)) {
				// check all the node that is not in the schedule
				boolean isProcessable = ScheduleHelper.isProcessable(n,
						branchingSchedule);
				if (isProcessable) {
					// if it is processable
					for (int i = 0; i < branchingSchedule.procLengths.length; i++) {
						// check all the available processor
                        int timeToWait = ScheduleHelper.checkChildNode(n, branchingSchedule, i);

//                        int tempCurrentProcLength = branchingSchedule.procLengths[i];
//                        tempCurrentProcLength += (int)Double.parseDouble(n.getAttribute("Weight").toString()) + timeToWait;
                        if (timeToWait > -1) {
							hasInserted = true;
							ScheduleHelper.insertNodeToSchedule(n, branchingSchedule, i, timeToWait);
							// Recursive
							Branch(new Schedule(branchingSchedule.schedule, branchingSchedule.procLengths, branchingSchedule.scheduleLength));
							branchingSchedule.removeNode(branchingSchedule.schedule.size() - 1);
							updateRemoveLengthChanges(branchingSchedule, n);
						}
					}
				}
			}
		}
		if (branchingSchedule.schedule.size() == g.getNodeCount()) {
			// no more children
			ScheduleHelper.foundNewBestSolution(branchingSchedule, g);
		}
		
		//If nothing was inserted, then go back up the tree.
		if (!hasInserted){
			return false;
		}

		//Return false to prevent while loop from exiting
		return false;
	
	}
	
	public void updateRemoveLengthChanges(Schedule s, Node removeNode){
		int updatedScheduleLength = 0;
		for (int i = s.schedule.size() -1 ; i > -1; i--) {
			Node n = s.schedule.get(i);
			int processedOn = (int)Double.parseDouble(n.getAttribute("Processor").toString());
			if (processedOn == (int)Double.parseDouble(removeNode.getAttribute("Processor").toString())) {
				s.procLengths[processedOn] = ScheduleHelper.getNodeWeight(g, n.getIndex()) + (int)Double.parseDouble(n.getAttribute("Start").toString());
				s.scheduleLength = s.findScheduleLength();
				updatedScheduleLength = 1;
				break;
			}
		}
		
		if (updatedScheduleLength == 0) {
			s.procLengths[(int)Double.parseDouble(removeNode.getAttribute("Processor").toString())] = 0;
			s.scheduleLength = s.findScheduleLength();
		}
	}
	
}
