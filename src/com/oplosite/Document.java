/**
 * 
 */
package com.oplosite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import utils.AlfrescoUtil;

/**
 * @author priyono
 *
 */
@ManagedBean(name = "document")
public class Document {

	// A variable to store file to be uploaded
	private UploadedFile file;
	private String folders;
	private static final String USER_HOME = "admin";

	public UploadedFile getFile() {
		return file;
	}

	public void setFile(UploadedFile file) {
		this.file = file;
	}

	/*
	 * Handling file upload when user press "upload" button
	 */
	public void fileHandler(FileUploadEvent event) throws IOException {
		
		System.out.println("uploading file");

		// Determine "local" temporary directory to build the file
		String filepath = "/tmp/" + event.getFile().getFileName();
		
		// Get the file object from any given file path. If the file is not existed yet
		// create a new one
		File file = new File(filepath);
		file.createNewFile();

		// Build the file by output stream
		FileOutputStream fos = new FileOutputStream(filepath);
		int read = 0;
		byte[] bytes = new byte[1024];
		InputStream is = event.getFile().getInputstream();
		
		while ((read = is.read(bytes)) != -1) {
			fos.write(bytes, 0, read);
		}

		fos.close();
		
		// Get newly created file content
		FileInputStream input = new FileInputStream(file);
	    
		System.out.println("file length: " + file.length());
		System.out.println("file name: " + file.getName());

		// Convert the newly created file content to multipart 
		MultipartFile multipartFile = new MockMultipartFile("file",
	            file.getName(), event.getFile().getContentType(), IOUtils.toByteArray(input));
		
		System.out.println("multipart size: " + multipartFile.getSize());
		System.out.println("saving file to Alfresco");
		
		// Call Alfresco util to upload the file to Alfresco
		AlfrescoUtil alfresco = new AlfrescoUtil();
		
		try {
			
			alfresco.upload(multipartFile, Document.USER_HOME);
			
			if (file != null) {
				FacesMessage message = new FacesMessage("Succesfully uploaded the file");
				FacesContext.getCurrentInstance().addMessage(null, message);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void init() throws Exception {
		System.out.println("init");
		AlfrescoUtil alfresco = new AlfrescoUtil();
		Folder rootFolder = alfresco.connect(alfresco.getFolderIdFromPath("/User Homes"));
		
		alfresco.listFolder(2, rootFolder);
	}

	/**
	 * @return the folders
	 */
	public String getFolders() {
		try {
			init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return folders;
	}

	/**
	 * @param folders the folders to set
	 */
	public void setFolders(String folders) {
		this.folders = folders;
	}
}
