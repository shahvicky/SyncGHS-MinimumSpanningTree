import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * @author shahvicky1992
 */
public class SyncGHS {

	final static Logger logger = Logger.getLogger(SyncGHS.class);
	int numOfRounds = 0;
	private int noOfAck;
	Edge  isCandidateForLeader;
	Edge coreEdge;
	boolean isOnCoreEdge;
	boolean isLocalMWOE;
	boolean algoTermination;

	public SyncGHS(int numOfNodes) {
		this.numOfRounds = (int) (Math.log(numOfNodes) + 1);
	}

	public void constructMST() {

		while (numOfRounds > 0) {
			isCandidateForLeader = new Edge();
			coreEdge = new Edge();
			isOnCoreEdge = false;
			isLocalMWOE = false;
			algoTermination = false;
			Edge receivedFromEdge = new Edge();

			if (Node.myId == Node.leaderId) {
				// broadcast SearchMWOEMSg along branch Edges
				Message searchMWOEMsg = createSearchMWOEMsg(Message_Type.SEARCH_MWOE);
				for (Edge edge : Node.branchEdges) {
					sendMessage(searchMWOEMsg, edge);
				}
			} else {
				/*
				 * wait for SearchMWOE_Msg and broadcast along branch edges
				 * except parent edge
				 */
				boolean receivedSearchMWOEMsg = false;
				while (!receivedSearchMWOEMsg) {
					if (!Node.buffer.isEmpty()) {
						for (Message msg : Node.buffer) {
							logger.debug("Received:  " + msg.toString());
							if (receivedSearchMWOEMsg) {
								break;
							}
							if (msg.getMsgType().equals(Message_Type.SEARCH_MWOE)) {
								receivedSearchMWOEMsg = true;
								/*
								 * broadcast SearchMWOEMSg along branch Edges
								 * except the one from which received
								 */
								receivedFromEdge = msg.getCurrentEdge();
								for (Edge edge : Node.branchEdges) {
									/*
									 * send search msg to its branch edges
									 * except receiver edge
									 */
									if (areSameEdges(edge, receivedFromEdge)) {
										continue;
									}
									noOfAck++;
									sendMessage(msg, edge);
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if (receivedSearchMWOEMsg) {
							break;
						}
					}
					// wait for some message to come or before going to the next
					// step
					logger.debug("Waiting for some time");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				}
			} // end of broadcasting SearchMWOEMsg

			// find its own MWOE
			Edge localMWOE = findLocalMWOE();
			Message replyMWOEMsg = createReplyMWOEMsg(Message_Type.REPLY_MWOE, localMWOE, Node.myId);
			// initialize to max value to find min
			Edge receivedMWOE = new Edge(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			Message receivedMWOEMsg = null;
			noOfAck = Node.branchEdges.size()-1;	//size()-1 as we won't get ack from receiver edge
			if (noOfAck == 0) { /*
								 * leaf node, no need to wait for ack, start
								 * convergecast of replyMWOEMsg along receiver
								 * edge
								 */
				sendMessage(replyMWOEMsg, receivedFromEdge);
			} else {

				while (noOfAck > 0) { /*
										 * receive REPLY_MWOE from all branch
										 * edges except receiver one.
										 */
					if (!Node.buffer.isEmpty()) {
						for (Message msg : Node.buffer) {
							if (noOfAck <= 0) {
								break;
							}
							/*
							 * check for replymwoemsg and find the min and send
							 * to receiver edge
							 */
							if (msg.getMsgType().equals(Message_Type.REPLY_MWOE)) {
								noOfAck--;
								/*
								 * updating mwoe to smallest of the received
								 * ones
								 */
								if (msg.getMwoeEdge().compareTo(receivedMWOE) < 0) {
									receivedMWOE = msg.getMwoeEdge();
									receivedMWOEMsg = msg;
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if (noOfAck <= 0) {
							break;
						}
					}
					// wait for some message to come or before going to the step
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				} // end of noOfAck while loop
				if (Node.myId != Node.leaderId) {
					if (receivedMWOE.compareTo(localMWOE) < 0) {
						sendMessage(receivedMWOEMsg, receivedFromEdge);
					} else {
						//sending localMWOE edge 
						sendMessage(replyMWOEMsg, receivedFromEdge);
					}
				}
			} // end of noOfAck else loop

			
			if (Node.myId == Node.leaderId) {
				if (receivedMWOE.compareTo(localMWOE) == 0) {
					logger.debug("No MWOE in the component");
					//exit from algorithm
					algoTermination = true;
				}
				else if (receivedMWOE.compareTo(localMWOE) < 0) {
					/* Broadcast ADD_MWOE to join on ids on received mwoe msg along branch edges*/
					Message addNWOEMsg = createAddMWOEMsg(Message_Type.ADD_MWOE, receivedMWOE);
					for(Edge edge: Node.branchEdges) {
						sendMessage(addNWOEMsg, edge);
					}
					
					/*also send null msg to basic edges which help in determining the node if it has received any JOIN message*/
					Message nullMsg = createNullMsg(Message_Type.NULL);
					for(Edge edge : Node.basicEdges) {
						sendMessage(nullMsg, edge);
					}
				} else {	//localMWOE is component MWOE
					Message addNWOEMsg = createAddMWOEMsg(Message_Type.ADD_MWOE, localMWOE);
					for(Edge edge: Node.branchEdges) {
						sendMessage(addNWOEMsg, edge);
					}
					addEdgeToBranchEdge(localMWOE);		//mark localMWOE as branch edge
					Message msg = createJoinMsg(Message_Type.JOIN);
					// send join msg along localMWOE
					sendMessage(msg, localMWOE);
					isLocalMWOE = true;
					copyObject(localMWOE, isCandidateForLeader);	//can be on core edge if receives a join
					//also send null msgs to basic edges
					Message nullMsg = createNullMsg(Message_Type.NULL);
					for(Edge edge : Node.basicEdges) {
						sendMessage(nullMsg, edge);
					}
				}
			} else {
				//node is not leader
				//wait for add_mwoe msg, first, broadcast this msg to branch edges except the one from which you received this,
				//then check if you have to add mwoe, 
				boolean receiverAddMWOEMsg = false;
				Message addMWOEMsg = null;
				while(!receiverAddMWOEMsg) {
					if(!Node.buffer.isEmpty()) {
						for(Message msg : Node.buffer) {
							if(receiverAddMWOEMsg) {
								break;
							}
							if(msg.getMsgType().equals(Message_Type.ADD_MWOE)) {
								receiverAddMWOEMsg = true;
								addMWOEMsg = msg;
								for(Edge edge: Node.branchEdges) {
									if(areSameEdges(msg.getCurrentEdge(), edge)) {		//do not send to receiver edge
										continue;
									}
									sendMessage(msg, edge);
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if(receiverAddMWOEMsg) {
							break;
						}
					}
					// wait for some message to come or before going to the step
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}
					
				}
				// if you have to add, then mark it branch edge 
				if(addMWOEMsg.getMwoeEdge().getMinId() == Node.myId || addMWOEMsg.getMwoeEdge().getMaxId() == Node.myId) {
					//add to branch edge and I have to send join request along this addMWOEMsg.getMwoeEdge()
					addEdgeToBranchEdge(addMWOEMsg.getMwoeEdge());
					Message msg = createJoinMsg(Message_Type.JOIN);
					sendMessage(msg, addMWOEMsg.getMwoeEdge());
					isLocalMWOE = true;
					copyObject(isCandidateForLeader, addMWOEMsg.getMwoeEdge());
				}
				// then finally broadcast null msgs to basic edges
				Message nullMsg = createNullMsg(Message_Type.NULL);
				for(Edge edge : Node.basicEdges) {
					sendMessage(nullMsg, edge);
				}
			}	//end of else part --> if I am not leader, then received ADD_MWOE msg
			
			//exit from while loop in no MWOE found
			if(algoTermination) {
				break;
			}
			
			/* check if you got any JOIN msg from any component, then mark that edge as branch edge. This can be done by counting the no of null msg received*/
			//this is common for all nodes, leader or non-leader
			int noOfNullJoinMsg;
			if(isLocalMWOE) {
				noOfNullJoinMsg = Node.basicEdges.size()+1;		//adding 1 as a basic edge was converted to branch edge and will either receive a join or null from it.
			} else {
				noOfNullJoinMsg = Node.basicEdges.size();
			}
			
			while(noOfNullJoinMsg > 0) {
				if(!Node.buffer.isEmpty()) {
					for(Message msg : Node.buffer) {
						if(noOfNullJoinMsg == 0) {
							break;
						}
						if(msg.getMsgType().equals(Message_Type.NULL)){
							noOfNullJoinMsg--;
							Node.buffer.remove(msg);
						} else if(msg.getMsgType().equals(Message_Type.JOIN)) {
							// add this current edge to branch edge
							addEdgeToBranchEdge(msg.currentEdge);
							noOfNullJoinMsg--;
							boolean isCoreEdge = areSameEdges(isCandidateForLeader, msg.currentEdge);
							if(isCoreEdge) {
								copyObject(isCandidateForLeader, coreEdge);
								isOnCoreEdge = true;
							}
							Node.buffer.remove(msg);
						} else {
							Node.buffer.offer(Node.buffer.poll());
						}
					}
					if(noOfNullJoinMsg == 0) {
						break;
					}
				}
				// wait for some message to come or before going to the step
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					logger.error(e);
				}
			}
			
			// NEW_LEADER algo
			
			if(isOnCoreEdge) {
				// if you are on core branch, update your leaderId, and broadcast on branchEdges except the core edge
				
				//update leader to maxId on core Edge
				Node.leaderId = coreEdge.getMaxId();
				//broadcast leader on branch edges except the core edge
				Message newLeaderMsg = createNewLeaderMsg(Message_Type.NEW_LEADER, Node.leaderId);
				for(Edge edge : Node.branchEdges) {
					if(areSameEdges(coreEdge, edge)) {
						continue;
					}
					sendMessage(newLeaderMsg, edge);
				}
			} else {
				//wait for new_leader msg, then update your leaderId and broadcast on branchEdges except the receiver edge
				boolean receivedNewLeaderMsg = false;
				
				while(!receivedNewLeaderMsg) {
					if(!Node.buffer.isEmpty()) {
						for(Message msg : Node.buffer) {
							if(receivedNewLeaderMsg) {
								break;
							}
							if(msg.getMsgType().equals(Message_Type.NEW_LEADER)) {
								receivedNewLeaderMsg = true;
								Node.leaderId = msg.getNewLeaderId();
								for(Edge edge : Node.branchEdges) {
									if(areSameEdges(msg.getCurrentEdge(), edge)) {
										continue;
									}
									sendMessage(msg, edge);
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if(receivedNewLeaderMsg) {
							break;
						}
					}
					// wait for some message to come or before going to the step
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				}
			}
			numOfRounds--;		//end of a phase
		} // end of while log(n) rounds
		
		logger.info(Node.branchEdges.toString());
	} // end of constructMST

	
	/**
	 * @param msgType
	 * @param newLeaderId
	 * @return
	 */
	private Message createNewLeaderMsg(Message_Type msgType, int newLeaderId) {
		Message msg = new Message(msgType);
		msg.setNewLeaderId(newLeaderId);
		return msg;
	}

	/**
	 * @param isCandidateForLeader2
	 * @param localMWOE
	 */
	private void copyObject(Edge sourceEdge, Edge destEdge) {
		//only these two properties are required to check a unique edge
		destEdge.setMaxId(sourceEdge.getMaxId());
		destEdge.setMinId(sourceEdge.getMinId());
	}
	
	private boolean areSameEdges(Edge edge1, Edge edge2) {
		if(edge1.getMinId()==edge2.getMinId() && edge1.getMaxId()==edge2.getMaxId()) {
			return true;
		}
		return false;
	}

	/**
	 * @param addMwoe
	 * @param receivedMWOEMsg
	 * @param receivedMWOE
	 * @return msg
	 */
	private Message createAddMWOEMsg(Message_Type msgType, Edge receivedMWOE) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		msg.setMwoeEdge(receivedMWOE);
		return msg;
	}

	/**
	 * @param null1
	 * @return
	 */
	private Message createNullMsg(Message_Type msgType) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		return msg;
	}

	/**
	 * @param localMWOE
	 */
	private void addEdgeToBranchEdge(Edge edge) {
		boolean errorCheck = false;
		//TODO check if it is already a branch edge
		Iterator<Edge> itr = Node.basicEdges.iterator();
		while(itr.hasNext()) {
			Edge basicEdge = itr.next();
			if(basicEdge.getMinId() == edge.getMinId() && basicEdge.getMaxId() == edge.getMaxId()) {
				basicEdge.setEdgeType(Edge_Type.BRANCH);
				Node.branchEdges.add(basicEdge);
				Node.basicEdges.remove(basicEdge);
				errorCheck = true;
				break;
			}
		}
		if(!errorCheck) {
			logger.error("Something wrong happened while changing basic edge to branch edge");
		}
	}

	/**
	 * @param msgType
	 * @return
	 */
	private Message createJoinMsg(Message_Type msgType) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		return msg;
	}

	/**
	 * @param msgType
	 * @param localMWOE
	 * @param mwoesource
	 * @return reply_mwoe_msg(leaderId, edge, mwoeSourceId, mwoeDestinationid)
	 */
	private Message createReplyMWOEMsg(Message_Type msgType, Edge localMWOE, int mwoeSourceId) {
		Message msg = new Message(msgType);
		msg.setMwoeEdge(localMWOE);
		msg.setLeaderId(Node.leaderId);
		msg.setMwoeSourceId(mwoeSourceId);
		int mwoeDestinationId = localMWOE.getMinId() == mwoeSourceId ? localMWOE.getMaxId() : localMWOE.getMinId();
		msg.setMwoeDestinationId(mwoeDestinationId);
		return msg;
	}

	/**
	 * @param msgType
	 * @return msg
	 */
	private Message createSearchMWOEMsg(Message_Type msgType) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		return msg;
	}

	public Edge findLocalMWOE() {
		boolean foundLocalMWOE = false;
		Edge mwoeEdge = null;
		while (!foundLocalMWOE) {
			// find MWOE => first edge in basic
			if (Node.basicEdges.size() > 0) {
				Edge candidateEdge = Node.basicEdges.get(0);
				// send Examine msg to selected MWOE to check if its not in the
				// same component
				Message examineMsg = createExamineMsg(Message_Type.EXAMINE);
				sendMessage(examineMsg, candidateEdge);
				String response = getExamineResponse();
				if (response.equals("REJECT")) {
					Edge rejectedEdge = Node.basicEdges.remove(0);
					addEdgeToRejectedEdges(rejectedEdge);
				} else if (response.equals("ACCEPT")) {
					mwoeEdge = candidateEdge;
					foundLocalMWOE = true;
				} else {
					logger.error("Something wrong happened in examine resonse");
				}
			} else {
				foundLocalMWOE = true;
				// no candidate edge and hence setting edge as infinity => new
				// Edge(weight, minId, maxId)
				mwoeEdge = new Edge(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
			}

		}

		return mwoeEdge;

	}

	/**
	 * @param rejectedEdge
	 */
	private void addEdgeToRejectedEdges(Edge rejectedEdge) {
		rejectedEdge.setEdgeType(Edge_Type.REJECTED);
		Node.rejectEdges.add(rejectedEdge);

	}

	/**
	 * @return
	 */
	private String getExamineResponse() {
		boolean receivedExamineResponseMsg = false;
		String examineResponse = null;
		while (!receivedExamineResponseMsg) {
			if (!Node.buffer.isEmpty()) {
				for (Message msg : Node.buffer) {
					if(receivedExamineResponseMsg) {
						break;
					}
					if (msg.getMsgType().equals(Message_Type.EXAMINE_RESPONSE)) {
						receivedExamineResponseMsg = true;
						examineResponse = msg.getExamineResponse();
						Node.buffer.remove(msg);
					} else {
						Node.buffer.offer(Node.buffer.poll());
					}
				}
				if(receivedExamineResponseMsg) {
					break;
				}
			}
			// wait for some message to come or before going to the next step
			logger.debug("Waiting for some time to receive examine response");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		return examineResponse;
	}

	/**
	 * @param msgType
	 * @return msg
	 */
	private Message createExamineMsg(Message_Type msgType) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		return msg;
	}

	public void sendMessage(Message msg, Edge edge) {
		if (edge == null) {
			return;
		}
		msg.setCurrentEdge(edge); // the edge from which the message is being
									// sent
		ObjectOutputStream outputStream = null;
		boolean scanning = true;
		while (scanning) {
			try {
				Socket clientSocket = new Socket(edge.getEdgeEndHostname(), edge.getEdgeEndPort());
				scanning = false;
				outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
			} catch (ConnectException e) {
				logger.error("ConnectException: failed with" + edge.getEdgeEndHostname() + " " + edge.getEdgeEndPort());
				try {
					Thread.sleep(2000);// wait for 2 seconds before trying next
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			} catch (UnknownHostException e) {
				logger.error("UnknownHostException" + e);
			} catch (IOException e) {
				logger.error("IOException" + e);
			}
		}
		try {
			outputStream.writeObject(msg);
			outputStream.reset();
			logger.debug(edge.getEdgeEndHostname() + ":" + edge.getEdgeEndPort() + " sent " + msg.toString());
		} catch (IOException e) {
			logger.error("IOException" + e);
		}
	}

}
