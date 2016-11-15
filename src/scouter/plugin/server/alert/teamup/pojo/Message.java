package scouter.plugin.server.alert.teamup.pojo;

public class Message {
	public Message(String content){
		this.content = content;
	}
	
	
	private String content;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}	
}
