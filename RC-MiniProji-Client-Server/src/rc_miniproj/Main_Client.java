package rc_miniproj;

//import java.util.Scanner;

public class Main_Client {

	public static void main(String[] argv) {
		//Scanner scanner = new Scanner(System.in);
		//System.out.print("Enter The Port : ");
		//int port = Long.pa
		AuthenticatedFileClient client = new AuthenticatedFileClient();
		client.start();
	}
}
