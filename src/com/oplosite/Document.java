/**
 * Name			:
 * Description	:
 */
package com.oplosite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.oplosite.models.ContentModel;

import utils.AlfrescoUtil;

/**
 * @author priyono
 *
 */
@ManagedBean(name = "document")
public class Document {

	// A variable to store file to be uploaded
	private String helloWorld = "Hello World!";

	// Silakan isi...
	private UploadedFile file;

	// Silakan isi...
	private ArrayList<ContentModel> contents;

	// Silakan isi...
	private StreamedContent downloadedFile;

	// Silakan isi...
	private static final String USER_HOME = "admin";

	/**
	 * @return the file
	 */
	public UploadedFile getFile() {
		return file;
	}

	/**
	 * @param file
	 */
	public void setFile(UploadedFile file) {
		this.file = file;
	}

	/**
	 * @return the helloWorld
	 */
	public String getHelloWorld() {
		return helloWorld;
	}

	/**
	 * @param helloWorld
	 *            the helloWorld to set
	 */
	public void setHelloWorld(String helloWorld) {
		this.helloWorld = helloWorld;
	}

	/**
	 * @return the folders
	 */
	public ArrayList<ContentModel> getContents() {
		return contents;
	}

	/**
	 * @param folders
	 *            the folders to set
	 */
	public void setContents(ArrayList<ContentModel> contents) {
		this.contents = contents;
	}

	/**
	 * @return the downloadedFile
	 * @throws Exception
	 */
	public StreamedContent getDownloadedFile() {
		return downloadedFile;
	}

	/**
	 * @param downloadedFile
	 *            the downloadedFile to set
	 */
	public void setDownloadedFile(StreamedContent downloadedFile) {
		this.downloadedFile = downloadedFile;
	}

	public void getAction() {
		System.out.println("action called");
	}

	/**
	 * Handling file upload when user press "upload" button
	 */
	public void fileHandler(FileUploadEvent event) throws IOException {

		System.out.println("uploading file");

		// Determine "local" temporary directory to build the file
		String filepath = "/tmp/" + event.getFile().getFileName();

		// Get the file object from any given file path. If the file is not
		// existed yet
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
		MultipartFile multipartFile = new MockMultipartFile("file", file.getName(), event.getFile().getContentType(),
				IOUtils.toByteArray(input));

		System.out.println("multipart size: " + multipartFile.getSize());
		System.out.println("saving file to Alfresco");

		// Call Alfresco util to upload the file to Alfresco
		AlfrescoUtil alfresco = new AlfrescoUtil();

		try {

			alfresco.upload(multipartFile, Document.USER_HOME);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Silakan isi deskripsi method ini di sini
	 * 
	 * @throws Exception
	 */
	public void listFolder(ActionEvent actionEvent) throws Exception {
		System.out.println(">> Listing folders");

		AlfrescoUtil alfresco = new AlfrescoUtil();
		String folderId = alfresco.getFolderIdFromPath("/User Homes/admin");

		contents = (ArrayList<ContentModel>) alfresco.listFolder(folderId);

		FacesMessage message = new FacesMessage("Succesfully listing folders");
		FacesContext.getCurrentInstance().addMessage(null, message);
	}

	/**
	 * Silakan isi deskripsi method ini di sini
	 * 
	 * @throws Exception
	 */
	public void download() throws Exception {
		System.out.println(">> Downloading file");

		AlfrescoUtil alfresco = new AlfrescoUtil();
		// String workspaceId = params.get("id");
		String workspaceId = "b3468e62-71b9-42ca-ac57-81edab93cff2"; // DUMMY
		Object[] doc = alfresco.download(workspaceId);
		InputStream stream = (InputStream) doc[0];
		downloadedFile = new DefaultStreamedContent(stream, (String) doc[1], (String) doc[2]);
	}
}
