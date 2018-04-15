
/**
 * @author shahvicky1992
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class FileParser{

	final static Logger logger = Logger.getLogger(FileParser.class);
	int numOfNodes;
	
	public ArrayList<Edge> readFile(String myHost){
		ArrayList<Edge> basicEdges = new ArrayList<>();
		File file = new File("C:\\Users\\shahvicky1992\\ProjectWorkspace\\Distributed\\SyncGHS\\src\\config1.txt");
		try( BufferedReader reader = new  BufferedReader(new FileReader(file)) ) {
			String line;
			/*read the number of nodes in the cofinguration*/
			while((line = reader.readLine()) != null) {
				if(!isLineValid(line))
					continue;
				numOfNodes = Integer.parseInt(line);
				Node.numOfNodes = numOfNodes;
				break;
			}
			logger.debug(numOfNodes);
			
			/*read the nodes id and the host and post info and store it as a hashmap
			 * as { id = [host, port]}*/
			HashMap<Integer, ArrayList<String>> id2HostPostMap = new HashMap<>();
			while((line = reader.readLine()) != null && numOfNodes>0) {
				if(!isLineValid(line))
					continue;
				String[] temp = line.split("\\s+");
				id2HostPostMap.put(Integer.parseInt(temp[0]), new ArrayList<String>());
				id2HostPostMap.get(Integer.parseInt(temp[0])).add(temp[1]);
				id2HostPostMap.get(Integer.parseInt(temp[0])).add(temp[2]);
				if(myHost.toLowerCase().equals(temp[1].toLowerCase())){
					Node.myId = Integer.parseInt(temp[0]);
					Node.myPort = Integer.parseInt(temp[2]);
					
				}
				numOfNodes--;
			}
			
			logger.debug(id2HostPostMap.toString());
			
			while((line = reader.readLine()) != null) {
				if(!isLineValid(line))
					continue;
				String[] edgeLine = null;
				edgeLine = line.split("\\s+");
				String edgeVertices = edgeLine[0].split("[\\(\\)]")[1];
				String idPair[]=edgeVertices.split(",");
				
				int weight= Integer.parseInt(edgeLine[1]);
				int minId = Integer.parseInt(idPair[0]);
				int maxId = Integer.parseInt(idPair[1]);
				
				basicEdges.add(new Edge(weight, minId, maxId));
			}
			logger.debug(basicEdges.toString());
			
			
		} catch (IOException e) {
			logger.info("File not found");
		}
		return basicEdges;
		
		
	}
	
	private boolean isLineValid(String line) {
		return line.length() > 0 && !line.startsWith("#");
	}
	
	
	
	
}
