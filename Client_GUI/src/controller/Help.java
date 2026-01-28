package controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import application.Main;

public class Help {
	
	public static void openLocalHtml() {
		try {
			// Get the URL of the resource
			URL resource = Main.class.getResource("/web/help.html");
			System.out.println("Page Web Help Exist : " + resource != null);
			if (resource != null) {
				// Convert the URL to a File object
				File file;
				try {
					file = new File(resource.toURI());
					if (file.exists() && Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(file);
						System.out.println("HTML file opened in your default browser.");
					} else {
						System.out
								.println("The file does not exist or your system does not support desktop operations.");
					}
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				System.out.println("Resource not found: /web/help.html");
			}
		} catch (IOException e) {
			System.out.println("An error occurred while trying to open the HTML file: " + e.getMessage());
		}
	}
}
