import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class Receiver implements Runnable {

	private Socket client;
	final static Logger logger = Logger.getLogger(Receiver.class);
	
	public Receiver(Socket client){
		this.client = client;
	}

	@Override
	public void run() {
		Message message;
		ObjectInputStream in;
		try{
			boolean addFlag = true;
			in = new ObjectInputStream(client.getInputStream());
			message = (Message) in.readObject();
			logger.debug(message.toString());
			// immediately send Reject if the Examine msg received from same component
			if(message.getMsgType().equals(Message_Type.EXAMINE)) {
				if(!(message.getPhaseNo() > Node.phase.intValue())) {
					addFlag= false;
					Message msg;
					synchronized (this) {
						Edge responseEdge = new Edge();
						for(Edge edge : Node.basicEdges) {
							//this loop is required to find proper endpoint host and post 
							if(areSameEdges(edge, message.getCurrentEdge())) {
								copyObject(edge, responseEdge);
							}
						}
						if(responseEdge.getMinId() == responseEdge.getMaxId()) {
							for(Edge edge : Node.branchEdges) {
								//this loop is required to find proper endpoint host and post 
								if(areSameEdges(edge, message.getCurrentEdge())) {
									copyObject(edge, responseEdge);
								}
							}
						}
						if(message.getLeaderId() == Node.leaderId){
							addEdgeToRejectedEdges(responseEdge);
							msg = createExamineResponseMsg(Message_Type.EXAMINE_RESPONSE, "REJECT");
							
						} else {
							msg = createExamineResponseMsg(Message_Type.EXAMINE_RESPONSE, "ACCEPT");
						}
						sendMessage(msg, responseEdge);
					}
				}
			}
			if(addFlag){
				Node.buffer.offer(message);
			}
		} catch(IOException e) {
			logger.error(e);
		} catch (ClassNotFoundException e) {
			logger.error(e);
		} 
		
	}

	/**
	 * @param currentEdge
	 */
	private synchronized void addEdgeToRejectedEdges(Edge currentEdge) {
		synchronized (Node.basicEdges) {
			Node.basicEdges.remove(currentEdge);
			currentEdge.setEdgeType(Edge_Type.REJECTED);
			Node.rejectEdges.add(currentEdge);
			/*Iterator<Edge> itr = Node.basicEdges.iterator();
			while(itr.hasNext()) {
				Edge edge = itr.next();
				if(edge.getMinId() == currentEdge.getMinId() && edge.getMaxId() == currentEdge.getMaxId()) {
					edge.setEdgeType(Edge_Type.REJECTED);
					Node.rejectEdges.add(edge);
					itr.remove();
					//Node.basicEdges.remove(edge);
				}
			}*/
		}
	}

	/**
	 * @param msgType
	 * @param string
	 * @return
	 */
	private Message createExamineResponseMsg(Message_Type msgType, String response) {
		Message msg = new Message(msgType);
		msg.setLeaderId(Node.leaderId);
		msg.setExamineResponse(response);
		return msg;
	}
	
	private boolean areSameEdges(Edge edge1, Edge edge2) {
		if (edge1.getMinId() == edge2.getMinId() && edge1.getMaxId() == edge2.getMaxId()) {
			return true;
		}
		return false;
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
	
	public void sendMessage(Message msg, Edge edge) {
		if (edge == null) {
			return;
		}
		msg.setPhaseNo(Node.phase.intValue());
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
			logger.debug(edge.getEdgeEndHostname()+ ":" + edge.getEdgeEndPort() + " sent "+ msg.toString());
		} catch (IOException e) {
			logger.error("IOException"+e);
		}
	}
	
}
