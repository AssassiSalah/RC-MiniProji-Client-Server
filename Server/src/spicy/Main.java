package spicy;

import spicy.User.FileWithVisibility;

public class Main {
    public static void main(String[] args) {
        UserManager userManager = new UserManager();
        int i =0;
        if (i==0)
        {      // Initialize random users
            userManager.initializeRandomUsers(5);

            // Display users
            userManager.displayUsers();
            
            for (User user : userManager.getUsers()) {
            	System.out.println("\ngetuserdata \n");
            	user.setListFiles_categorizeFilesByVisibility(user.getListFiles());
            	for (FileWithVisibility str :user.getFilesWithVisibility())
            	{
                	System.out.print( str.getFileName()+" ");

            	}
				
			}
            
        	
        }
        else 
        {
            
            userManager.displayUsers();
        }
  
        

    }
}
