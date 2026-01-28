package check_virus;

import java.io.File;

public class CheckVirus {
	
	public static boolean isSafe(File file) {
		
		if(!file.exists()) {
			System.out.println("File Not Found.");
    		return false;
    	}
		
		try {
			if(CheckInternet.isOnline()) {
				System.out.println("Check With API");
				return CheckWithAPI.isSafe(file);
			} else 
				if (isWindows() && CheckWithDefender.isWindowsDefenderAvailable()) {
						System.out.println("Check With Windows Defender");
						return CheckWithDefender.scanFileWithDefender(file.getAbsolutePath());//TODO FIX THIS
			}
		} catch(Exception e) {
			System.out.println("Error scanning file with Windows Defender: " + e.getMessage());
            System.out.println("Some files may be infected. Please review the logs.");
		}
		System.out.println("Can't Check");
		return false; // Can't Check The File
	}
	
	public static boolean isWindows() {
    	//String os = System.getProperty("os.name");
        //System.out.println("Operating System: " + os);
    	return System.getProperty("os.name").contains("Windows");
	}

}