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
	Edge isCandidateForLeader;
	Edge coreEdge;
	boolean isOnCoreEdge;
	boolean isLocalMWOE;
	boolean algoTermination;

	public SyncGHS(int numOfNodes) {
		this.numOfRounds = (int) (Math.log(numOfNodes) + 1);
	}

	public void constructMST() {

		while (numOfRounds > 0) {
			noOfAck = 0;
			isCandidateForLeader = new Edge();
			coreEdge = new Edge();
			isOnCoreEdge = false;
			isLocalMWOE = false;
			algoTermination = false;
			Edge receivedFromEdge = new Edge();

			if (Node.myId == Node.leaderId) {
				// leader broadcast SearchMWOEMSg along branch Edges
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
					logger.debug("Waiting for SEARCH_WMOE Msg");
					if (!Node.buffer.isEmpty()) {
						for (Message msg : Node.buffer) {
							logger.debug("Received:  " + msg.toString());
							if (receivedSearchMWOEMsg) {
								break;
							}
							if (msg.getMsgType().equals(Message_Type.SEARCH_MWOE)) {
								receivedSearchMWOEMsg = true;
								logger.debug("Received Search_MWOE Msg");
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
										//receivedFromEdge from Node's perspective
										receivedFromEdge = edge;
										continue;
									}
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
					logger.debug("Still Waiting for SEARCH_WMOE Msg");
					try {
						Thread.sleep(500);
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
			if(Node.myId == Node.leaderId) {
				noOfAck = Node.branchEdges.size();
			} else {
				/*size()-1 as we won't get ack from receiver edge */
				noOfAck = Node.branchEdges.size() - 1; 
			}
			if (noOfAck == 0 && Node.myId != Node.leaderId) { /*
								 * leaf node, no need to wait for ack, start
								 * convergecast of replyMWOEMsg along receiver
								 * edge
								 */
				if(receivedFromEdge == null) {
					logger.error("Parent Edge null.. Problem");
				}
				sendMessage(replyMWOEMsg, receivedFromEdge);
			} else {
				/*
				 * receive REPLY_MWOE from all branch
				 * edges except receiver one.
				 */
				while (noOfAck > 0) { 
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
								logger.debug("Received REPLY_MWOE Msg");
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
					logger.debug("Waiting for REPLY_WMOE Msg");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				} // end of noOfAck while loop
				if (Node.myId != Node.leaderId) {
					if (receivedMWOE.compareTo(localMWOE) == 0) {
						logger.debug("No MWOE for me and my children");
						sendMessage(receivedMWOEMsg, receivedFromEdge);
					}
					else if (receivedMWOE.compareTo(localMWOE) < 0) {
						logger.debug("Received MWOE is less than local MWOE");
						sendMessage(receivedMWOEMsg, receivedFromEdge);
					} else {
						logger.debug("Sending Local MWOE");
						// sending localMWOE edge
						sendMessage(replyMWOEMsg, receivedFromEdge);
					}
				}
			} // end of noOfAck else loop

			if (Node.myId == Node.leaderId) {
				if (receivedMWOE.compareTo(localMWOE) == 0) {
					logger.debug("No MWOE in the component");
					// exit from algorithm
					algoTermination = true;
				} else if (receivedMWOE.compareTo(localMWOE) < 0) {
					logger.debug("Received MWOE is least of the component");
					/*
					 * Broadcast ADD_MWOE to join on ids on received mwoe msg
					 * along branch edges
					 */
					logger.debug("Start of ADD_MWOE broadcast");
					Message addMWOEMsg = createAddMWOEMsg(Message_Type.ADD_MWOE, receivedMWOE);
					for (Edge edge : Node.branchEdges) {
						sendMessage(addMWOEMsg, edge);
					}

					/*
					 * also send null msg to basic edges which help in
					 * determining the node if it has received any JOIN message
					 */
					Message nullMsg = createNullMsg(Message_Type.NULL);
					for (Edge edge : Node.basicEdges) {
						sendMessage(nullMsg, edge);
					}
				} else { // localMWOE is component MWOE
					logger.debug("Leader's local MWOE is the least of the component");
					Message addMWOEMsg = createAddMWOEMsg(Message_Type.ADD_MWOE, localMWOE);
					for (Edge edge : Node.branchEdges) {
						sendMessage(addMWOEMsg, edge);
					}
					// mark localMWOE as branch edge
					addEdgeToBranchEdge(localMWOE); 
					Message msg = createJoinMsg(Message_Type.JOIN);
					// send join msg along localMWOE
					sendMessage(msg, localMWOE);
					isLocalMWOE = true;
					// can be on core edge if receives a join
					copyObject(localMWOE, isCandidateForLeader);
					logger.debug("isCandidateForLeader....."+ isCandidateForLeader.toString());
					// also send null msgs to basic edges
					Message nullMsg = createNullMsg(Message_Type.NULL);
					for (Edge edge : Node.basicEdges) {
						sendMessage(nullMsg, edge);
					}
				}
			} else {
				// node is not leader
				// wait for add_mwoe msg, first, broadcast this msg to branch
				// edges except the one from which you received this,
				// then check if you have to add mwoe,
				boolean receivedAddMWOEMsg = false;
				Message addMWOEMsg = null;
				while (!receivedAddMWOEMsg) {
					if (!Node.buffer.isEmpty()) {
						for (Message msg : Node.buffer) {
							if (receivedAddMWOEMsg) {
								break;
							}
							if (msg.getMsgType().equals(Message_Type.ADD_MWOE)) {
								logger.debug("Received ADD_MWOE Msg");
								receivedAddMWOEMsg = true;
								addMWOEMsg = msg;
								logger.debug("Sending ADD_MWOE Msg to child edges");
								for (Edge edge : Node.branchEdges) {
									if (areSameEdges(msg.getCurrentEdge(), edge)) {
										/* do not send to receiver edge */
										continue;
									}
									sendMessage(msg, edge);
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if (receivedAddMWOEMsg) {
							break;
						}
					}
					// wait for some message to come or before going to the step
					logger.debug("Waiting for ADD_MWOE Msg");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}

				}
				// if you have to add, then mark it branch edge
				if (addMWOEMsg.getMwoeEdge().getMinId() == Node.myId
						|| addMWOEMsg.getMwoeEdge().getMaxId() == Node.myId) {
					logger.debug("I have to add component MWOE");
					// add to branch edge and I have to send join request along
					// this addMWOEMsg.getMwoeEdge()
					Edge mwoe = new Edge();
					for(Edge edge : Node.basicEdges) {
						//this loop is required to find proper endpoint host and post 
						if(areSameEdges(edge, addMWOEMsg.getMwoeEdge())) {
							copyObject(edge, mwoe);
						}
					}
					if(mwoe.getMinId() == mwoe.getMaxId()) {
						for(Edge edge : Node.branchEdges) {
							//this loop is required to find proper endpoint host and post 
							if(areSameEdges(edge, addMWOEMsg.getCurrentEdge())) {
								copyObject(edge, mwoe);
							}
						}
					}
					addEdgeToBranchEdge(mwoe);
					Message msg = createJoinMsg(Message_Type.JOIN);
					sendMessage(msg, mwoe);
					isLocalMWOE = true;
					copyObject(isCandidateForLeader, mwoe);
				}
				// then finally broadcast null msgs to basic edges
				Message nullMsg = createNullMsg(Message_Type.NULL);
				for (Edge edge : Node.basicEdges) {
					sendMessage(nullMsg, edge);
				}
			} /*
				 * end of else part --> if I am not leader, then received ADD_MWOE
				 * msg
				 */

			// exit from while loop in no MWOE found
			if (algoTermination) {
				break;
			}

			/*
			 * check if you got any JOIN msg from any component, then mark that
			 * edge as branch edge. This can be done by counting the no of null
			 * msg or join msgs received
			 */
			// this is common for all nodes, leader or non-leader
			int noOfNullJoinMsg;
			if (isLocalMWOE) {
				/*
				 * adding 1 as a basic edge was converted to branch edge and
				 * will either receive a join or null from it.
				 */
				noOfNullJoinMsg = Node.basicEdges.size() + 1;
			} else {
				noOfNullJoinMsg = Node.basicEdges.size();
			}
			logger.debug("noOfNullJoinMsg  ... " + noOfNullJoinMsg);

			while (noOfNullJoinMsg > 0) {
				if (!Node.buffer.isEmpty()) {
					for (Message msg : Node.buffer) {
						if (noOfNullJoinMsg == 0) {
							break;
						}
						if (msg.getMsgType().equals(Message_Type.NULL)) {
							noOfNullJoinMsg--;
							Node.buffer.remove(msg);
						} else if (msg.getMsgType().equals(Message_Type.JOIN)) {
							// add this current edge to branch edge
							Edge joinEdge = new Edge();
							for(Edge edge : Node.basicEdges) {
								//this loop is required to find proper endpoint host and post 
								if(areSameEdges(edge, msg.getCurrentEdge())) {
									copyObject(edge, joinEdge);
								}
							}
							if(joinEdge.getMinId() == joinEdge.getMaxId()) {
								for(Edge edge : Node.branchEdges) {
									//this loop is required to find proper endpoint host and post 
									if(areSameEdges(edge, msg.getCurrentEdge())) {
										copyObject(edge, joinEdge);
									}
								}
							}
							logger.debug("join edge..." + joinEdge.toString());
							addEdgeToBranchEdge(joinEdge);
							noOfNullJoinMsg--;
							boolean isCoreEdge = areSameEdges(isCandidateForLeader, joinEdge);
							if (isCoreEdge) {
								logger.debug("I am on core edge");
								copyObject(isCandidateForLeader, coreEdge);
								isOnCoreEdge = true;
							}
							Node.buffer.remove(msg);
						} else {
							Node.buffer.offer(Node.buffer.poll());
						}
					}
					if (noOfNullJoinMsg == 0) {
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

			if (isOnCoreEdge) {
				// if you are on core branch, update your leaderId, and
				// broadcast on branchEdges except the core edge
				// update leader to maxId on core Edge
				Node.leaderId = coreEdge.getMaxId();
				// broadcast leader on branch edges except the core edge
				Message newLeaderMsg = createNewLeaderMsg(Message_Type.NEW_LEADER, Node.leaderId);
				logger.debug("Broadcasting NEW_LEADER Msg");
				for (Edge edge : Node.branchEdges) {
					if (areSameEdges(coreEdge, edge)) {
						continue;
					}
					sendMessage(newLeaderMsg, edge);
				}
			} else {
				// wait for new_leader msg, then update your leaderId and
				// broadcast on branchEdges except the receiver edge
				boolean receivedNewLeaderMsg = false;

				while (!receivedNewLeaderMsg) {
					if (!Node.buffer.isEmpty()) {
						for (Message msg : Node.buffer) {
							if (receivedNewLeaderMsg) {
								break;
							}
							if (msg.getMsgType().equals(Message_Type.NEW_LEADER)) {
								receivedNewLeaderMsg = true;
								Node.leaderId = msg.getNewLeaderId();
								for (Edge edge : Node.branchEdges) {
									if (areSameEdges(msg.getCurrentEdge(), edge)) {
										continue;
									}
									sendMessage(msg, edge);
								}
								Node.buffer.remove(msg);
							} else {
								Node.buffer.offer(Node.buffer.poll());
							}
						}
						if (receivedNewLeaderMsg) {
							break;
						}
					}
					// wait for some message to come or before going to the step
					logger.info("Waiting for NEW_LEADER Msg");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						logger.error(e);
					}
				}
			}
			numOfRounds--; // end of a phase
		} // end of while log(n) rounds

		logger.info("Result" + Node.branchEdges.toString());
	} // end of constructMST

	/**
	 * @param msgType
	 * @param newLeaderId
	 * @return
	 */
	private Message createNewLeaderMsg(Message_Type msgType, int newLeaderId) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		msg.setNewLeaderId(newLeaderId);
		return msg;
	}

	/**
	 * @param sourceEdge
	 * @param destEdge
	 */
	private void copyObject(Edge sourceEdge, Edge destEdge) {
		// only minId and maxId properties are required to check a unique edge
		destEdge.setMaxId(sourceEdge.getMaxId());
		destEdge.setMinId(sourceEdge.getMinId());
		destEdge.setWeight(sourceEdge.getWeight());
		destEdge.setEdgeType(sourceEdge.getEdgeType());
		// copy the host and port of the end point
		destEdge.setEdgeEndHostname(sourceEdge.getEdgeEndHostname());
		destEdge.setEdgeEndPort(sourceEdge.getEdgeEndPort());
	}

	private boolean areSameEdges(Edge edge1, Edge edge2) {
		if (edge1.getMinId() == edge2.getMinId() && edge1.getMaxId() == edge2.getMaxId()) {
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
		Iterator<Edge> itr = Node.basicEdges.iterator();
		while (itr.hasNext()) {
			Edge basicEdge = itr.next();
			if (areSameEdges(basicEdge, edge)) {
				basicEdge.setEdgeType(Edge_Type.BRANCH);
				Node.branchEdges.add(basicEdge);
				itr.remove();
				errorCheck = true;
				return;
			}
		}
		if (!errorCheck) {
			Iterator<Edge> it = Node.branchEdges.iterator();
			while (it.hasNext()) {
				if (areSameEdges(it.next(), edge)) {
					logger.debug("Already marked as branch");
					return;
				}
			}
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
		logger.debug("Finding local MWOE");
		boolean foundLocalMWOE = false;
		Edge mwoeEdge = null;
		while (!foundLocalMWOE) {
			// find MWOE => first edge in basic
			if (!Node.basicEdges.isEmpty()) {
				Edge candidateEdge = Node.basicEdges.get(0);
				// send Examine msg to selected MWOE to check if its not in the
				// same component
				Message examineMsg = createExamineMsg(Message_Type.EXAMINE);
				logger.debug("Sending Examine Msg");
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
				logger.debug("No local MWOE");
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
					if (receivedExamineResponseMsg) {
						break;
					}
					if (msg.getMsgType().equals(Message_Type.EXAMINE_RESPONSE)) {
						logger.debug("Received EXAMINE_RESPONSE");
						receivedExamineResponseMsg = true;
						examineResponse = msg.getExamineResponse();
						Node.buffer.remove(msg);
					} else {
						Node.buffer.offer(Node.buffer.poll());
					}
				}
				if (receivedExamineResponseMsg) {
					break;
				}
			}
			// wait for some message to come or before going to the next step
			logger.debug("Waiting to receive EXAMINE_RESPONSE Msg");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		if(examineResponse == null) {
			logger.error("Problem in receiving EXAMINE_RESPONSE Msg");
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
