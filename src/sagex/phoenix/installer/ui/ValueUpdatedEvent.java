package sagex.phoenix.installer.ui;

public class ValueUpdatedEvent {
	public ValueUpdatedEvent(String id2, Object value2) {
		this.id=id2;
		this.value=value2;
	}
	public String id;
	public Object value;
}
