package rc_miniproj;

public class Main_Server {

	public static void main(String[] args) {
		AuthenticatedFileServer server = new AuthenticatedFileServer();
		server.start();

	}

}
