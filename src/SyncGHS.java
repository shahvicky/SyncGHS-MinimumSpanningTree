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
	int numOfRounds;

	public SyncGHS(int numOfNodes) {
		this.numOfRounds = (int) (Math.log(numOfNodes) + 1);
	}

	public void constructMST() {

		while (numOfRounds > 0) {

			if (Node.myId == Node.leaderId) {
				// broadcast SearchMWOEMSg along branch Edges and find its own
				// MWOE
				// find MWOE => first edge in basic
				// send Examine msg to selected MWOE to check if its not in the
				// same component
				// immediately seng Reject if the Examine msg received from same
				// component
				// wait for other MWOE number of children SearchMWOEMsg
			} else {
				// wait for SearchMWOE_Msg
			}

		}
	}

	public Edge findMWOE() {

		return null;

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
