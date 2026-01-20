package hash_file;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class Main {

	public static void main(String[] args)  {
		File file = chooseFile();
		
		System.out.println(FileHasher.hashFile(file));
	}
	
	private static File chooseFile()
	{
		  JFrame frame = new JFrame();
	      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	      JFileChooser fileChooser = new JFileChooser();
	      int result = fileChooser.showOpenDialog(frame);
	      
	      if (result == JFileChooser.APPROVE_OPTION)
	          return fileChooser.getSelectedFile();
	      
        return null;
	}
}
