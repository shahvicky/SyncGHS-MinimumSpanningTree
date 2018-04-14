
/**
 * @author shahvicky1992
 */
// broadcast by current leader on tree edges
public class SearchMWOEMsg {

	Message_Type msgType;
	int leaderId;
	int level;
	
	public SearchMWOEMsg(int leader, int level) {
		this.leaderId = leader;
		this.level = level;
		this.msgType = Message_Type.SEARCH_MWOE;
	}

	@Override
	public String toString() {
		return "SearchMWOEMsg [msgType=" + msgType + ", leader=" + leaderId + ", level=" + level + "]";
	}
	
}
