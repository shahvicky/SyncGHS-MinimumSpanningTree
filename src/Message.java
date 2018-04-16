import java.io.Serializable;

/**
 * @author shahvicky1992
 */
enum Message_Type {
	SEARCH_MWOE, EXAMINE, EXAMINE_RESPONSE, REPLY_MWOE, ADD_MWOE, NEW_LEADER
}

public class Message implements Serializable{
	
	int leaderId;
	Message_Type msgType;
	int sourceId;
	int destinationId;
	String examineResponse;		//ACCEPT-if different leader	 or REJECT-if same leader
	Edge edge;

	public Message(Message_Type msgType) {
		this.msgType = msgType;
	}
	
	public int getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(int leaderId) {
		this.leaderId = leaderId;
	}

	public Message_Type getMsgType() {
		return msgType;
	}

	public void setMsgType(Message_Type msgType) {
		this.msgType = msgType;
	}

	public int getSourceId() {
		return sourceId;
	}

	public void setSourceId(int sourceId) {
		this.sourceId = sourceId;
	}

	public int getDestinationId() {
		return destinationId;
	}

	public void setDestinationId(int destinationId) {
		this.destinationId = destinationId;
	}

	public String getExamineResponse() {
		return examineResponse;
	}

	public void setExamineResponse(String examineResponse) {
		this.examineResponse = examineResponse;
	}

	public Edge getEdge() {
		return edge;
	}

	public void setEdge(Edge edge) {
		this.edge = edge;
	}

	
	/**
	 * @param leaderId
	 * @param searchMwoe
	 */
	public Message(int leaderId, Message_Type msgType) {
		this.leaderId = leaderId;
		this.msgType = msgType;
	}

	/**
	 * @param examineResponse2
	 * @param string
	 */
	public Message(Message_Type msgType, String examineResponse) {
		this.msgType = msgType;
		this.examineResponse = examineResponse;
	}

	public Message createMsg(Message_Type msgType) {
		switch (msgType) {
		case SEARCH_MWOE:
			return searchMWOEMsg();
		case EXAMINE:
			return examineMsg();
		case EXAMINE_RESPONSE:
			return examineResponseMsg();
		case REPLY_MWOE:
			return replyMWOEMsg();
		case ADD_MWOE:
			return addMWOEMsg();
		case NEW_LEADER:
			return newLeaderMsg();
		default:
			return null;
		}
	}
	
	public Message createResponseMsg(Message msg) {
		if(msg.msgType.equals(Message_Type.EXAMINE)){
			if(msg.leaderId == Node.leaderId){
				return new Message(Message_Type.EXAMINE_RESPONSE, "REJECT");
			} else {
				return new Message(Message_Type.EXAMINE_RESPONSE, "ACCEPT");
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	private Message newLeaderMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	private Message addMWOEMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	private Message replyMWOEMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	private Message examineResponseMsg() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return
	 */
	private Message examineMsg() {
		return new Message(Node.leaderId, Message_Type.SEARCH_MWOE);
	}

	/**
	 * @return
	 */
	private Message searchMWOEMsg() {
		return new Message(Node.leaderId, Message_Type.SEARCH_MWOE);
	}

}
