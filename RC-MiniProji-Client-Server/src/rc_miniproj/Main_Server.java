package rc_miniproj;

//import java.util.Scanner;

public class Main_Server {

	public static void main(String[] args) {
		//try (Scanner scanner = new Scanner(System.in)) {
			
			//long currentSize = 139799210;
			//long totalSize = 139799880;
			//System.out.println((double) currentSize / totalSize + "%");
			//System.out.println((double) t / j + "%");
			
			//System.out.print("Enter The Port : ");
			//int port = Integer.parseInt(scanner.nextLine());
			
			AuthenticatedFileServer server = new AuthenticatedFileServer(1234);
			server.start();
		//} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
	}
}
