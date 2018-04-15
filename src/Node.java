	
/**
 * @author shahvicky1992
 */
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

public class Node {
	
	final static Logger logger = Logger.getLogger(Node.class);
	static int myId;
	static String myHost;
	static int myPort;
	static int leaderId;
	static int level;
	static int numOfNodes;
	static ArrayList<Edge> basicEdges;
	static ArrayList<Edge> branchEdges;
	static ArrayList<Edge> rejectEdges;
	
	public static void main(String[] args) throws IOException {
		
		InetAddress inetAddress = InetAddress.getLocalHost();
		myHost = inetAddress.getHostName();
		
		FileParser parser = new FileParser();
		ArrayList<Edge> edgeList = parser.readFile(myHost);
		logger.debug("MyID"+myId+".......MyHost"+myHost+"......MyPort"+myPort);
		logger.debug(edgeList.toString());
		Collections.sort(edgeList);
		logger.debug(edgeList.toString());
		
	}
}
