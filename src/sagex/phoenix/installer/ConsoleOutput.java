package sagex.phoenix.installer;

public class ConsoleOutput implements IOutput {
	@Override
	public void msg(String msg) {
		System.out.println("CONSOLE: " + msg);
	}
}
