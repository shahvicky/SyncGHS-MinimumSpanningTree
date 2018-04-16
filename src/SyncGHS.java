import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

/**
 * @author shahvicky1992
 */
public class SyncGHS {

	final static Logger logger = Logger.getLogger(SyncGHS.class);
	int numOfRounds = 0;
	private int noOfAck = 0;

	public SyncGHS(int numOfNodes) {
		this.numOfRounds = (int) (Math.log(numOfNodes) + 1);
	}

	public void constructMST() {

		while (numOfRounds > 0) {
			Edge receivedFromEdge = null;

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
									if (edge.getMinId() == receivedFromEdge.getMinId()) {
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
							 * check for replymwoemsg and find the min and end
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
						logger.debug(e);
					}
				} // end of noOfAck while loop
			} // end of noOfAck else loop

			if (Node.myId != Node.leaderId) {
				if (receivedMWOE.compareTo(localMWOE) < 0) {
					sendMessage(receivedMWOEMsg, receivedFromEdge);
				} else {
					sendMessage(replyMWOEMsg, receivedFromEdge);
				}
			}
			if (Node.myId == Node.leaderId) {
				if (receivedMWOE.compareTo(localMWOE) < 0) {
					// TODO Broadcast ADD_MWOE to join on ids on received mwoe
					// msg
				} else {
					// TODO ADD_MWOE itself and also broadcast so that others
					// can mark there basic to branch on which they have
					// received ADD_MWOE
				}
			}
			
			//TODO NEW_LEADER algo

		} // end of while log(n) rounds
	} // end of constructMST

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
					addEdgeToRejectedEdged(rejectedEdge);
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
	private void addEdgeToRejectedEdged(Edge rejectedEdge) {
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
					if (msg.getMsgType().equals(Message_Type.EXAMINE_RESPONSE)) {
						receivedExamineResponseMsg = true;
						examineResponse = msg.getExamineResponse();
						Node.buffer.remove(msg);
					} else {
						Node.buffer.offer(Node.buffer.poll());
					}
				}
			}
			// wait for some message to come or before going to the next step
			logger.debug("Waiting for some time");
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
