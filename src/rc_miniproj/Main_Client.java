package rc_miniproj;

public class Main_Client {

	public static void main(String[] argv) {
		AuthenticatedFileClient client = new AuthenticatedFileClient();
		client.start();
	}
}
