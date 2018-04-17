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
				addFlag= false;
				Message msg;
				synchronized (this) {
					if(message.getLeaderId() == Node.leaderId){
						addEdgeToRejectedEdges(message.getCurrentEdge());
						msg = createExamineResponseMsg(Message_Type.EXAMINE_RESPONSE, "REJECT");
						
					} else {
						msg = createExamineResponseMsg(Message_Type.EXAMINE_RESPONSE, "ACCEPT");
					}
					sendMessage(msg, message.getCurrentEdge());
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
			Iterator<Edge> itr = Node.basicEdges.iterator();
			while(itr.hasNext()) {
				Edge edge = itr.next();
				if(edge.getMinId() == currentEdge.getMinId() && edge.getMaxId() == currentEdge.getMaxId()) {
					edge.setEdgeType(Edge_Type.REJECTED);
					Node.rejectEdges.add(edge);
					itr.remove();
					//Node.basicEdges.remove(edge);
				}
			}
		}
	}

	/**
	 * @param msgType
	 * @param string
	 * @return
	 */
	private Message createExamineResponseMsg(Message_Type msgType, String response) {
		Message msg = new Message(msgType);
		msg.setExamineResponse(response);
		return msg;
	}
	
	public void sendMessage(Message msg, Edge edge) {
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
