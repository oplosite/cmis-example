package utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;

import com.oplosite.models.ContentModel;

public class AlfrescoUtil {

	/**
	 * Host domain and port with a trailing slash
	 */
	public static final String ALFRESCO_API_URL = "http://localhost:8080/";

	/**
	 * Username of a user with write access to the FOLDER_PATH
	 */
	public static final String USER_NAME = "admin";

	/**
	 * Password of a user with write access to the FOLDER_PATH
	 */
	public static final String PASSWORD = "admin";

	/**
	 * Folder path
	 */
	public static final String FOLDER_PATH = "/User Homes";

	// Probably do not need to change any constants below this

	// public static final String ATOMPUB_URL = ALFRESCO_API_URL +
	// "alfresco/cmisatom"; // 4.0 - 4.2c
	public static final String ATOMPUB_URL = ALFRESCO_API_URL + "alfresco/api/-default-/public/cmis/versions/1.0/atom"; // 4.2d

	public Session createSession() throws Exception {
		// Default factory implementation of client runtime.
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameter = new HashMap<>();
		System.out.println(AlfrescoUtil.ATOMPUB_URL);
		// User credentials.
		parameter.put(SessionParameter.USER, AlfrescoUtil.USER_NAME);
		parameter.put(SessionParameter.PASSWORD, AlfrescoUtil.PASSWORD);

		// Connection settings.
		// URL to CMIS server
		parameter.put(SessionParameter.ATOMPUB_URL, AlfrescoUtil.ATOMPUB_URL);
		// parameter.put(SessionParameter.REPOSITORY_ID, "myRepository"); //Only
		// necessary if there is more than one repository.
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

		// This supposes only one repository is available at the URL.
		Repository soleRepository = sessionFactory.getRepositories(parameter).get(0);
		// create session
		Session session = soleRepository.createSession();

		return session;
	}

	public String upload(MultipartFile file, String folderpath) throws Exception {
		Session session = createSession();
		// define fullpath to folder
		String fullpath = AlfrescoUtil.FOLDER_PATH;
		if (!folderpath.trim().equals("")) {
			fullpath = fullpath + "/" + folderpath;
		}

		// get folder by fullpath
		Folder folder = (Folder) session.getObjectByPath(fullpath);
		// separate filename and the extension, and get unique filename
		String name = FilenameUtils.getBaseName(file.getOriginalFilename());
		String ext = FilenameUtils.getExtension(file.getOriginalFilename());
		String filename = name + "." + ext;

		// properties
		// (minimal set: name and object type id)
		Map<String, Object> properties = new HashMap<>();
		properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		properties.put(PropertyIds.NAME, filename);

		// content
		InputStream stream = new ByteArrayInputStream(file.getBytes());
		ContentStream contentStream = new ContentStreamImpl(file.getOriginalFilename(),
				BigInteger.valueOf(file.getBytes().length), file.getContentType(), stream);

		// create a major version
		Document newDoc = folder.createDocument(properties, contentStream, VersioningState.MAJOR);
		String id = newDoc.getId();
		session.clear();

		return id;
	}

	public Object[] download(String id) throws Exception {
		Session session = createSession();
		Object[] result = new Object[3];
		Document document = (Document) session.getObject(session.createObjectId(id));
		InputStream stream = document.getContentStream().getStream();
		String type = document.getContentStream().getMimeType();
		String filename = document.getName();

		result[0] = stream;
		result[1] = type;
		result[2] = filename;

		return result;
	}

	Object[] downloadByPath(String path) throws Exception {
		Session session = createSession();
		Object[] result = new Object[3];
		Document document = (Document) session.getObjectByPath(path);
		InputStream stream = document.getContentStream().getStream();
		String type = document.getContentStream().getMimeType();
		String filename = document.getName();
		result[0] = stream;
		result[1] = type;
		result[2] = filename;

		return result;
	}

	boolean delete(String id) throws Exception {
		// Create session.
		Session session = createSession();
		Document document = (Document) session.getObject(id);
		document.delete();
		session.clear();
		return true;
	}

	String getPathFromId(String id) throws Exception {
		Session session = createSession();
		Document document = (Document) session.getObject(id);
		List<String> paths = document.getPaths();
		String path = paths.get(0);

		session.clear();

		return path;
	}

	public String getIdFromPath(String path) throws Exception {
		Session session = createSession();
		Document document = (Document) session.getObjectByPath(AlfrescoUtil.ALFRESCO_API_URL + "/" + path);
		String id = document.getId();

		session.clear();

		return id;
	}

	String createFolder(String folderpath, String foldername) throws Exception {
		Session session = createSession();

		String fullpath = AlfrescoUtil.FOLDER_PATH;
		if (!folderpath.trim().equals("")) {
			fullpath = fullpath + "/" + folderpath;
		}
		Folder folder = (Folder) session.getObjectByPath(fullpath);

		// cek nama folder if existing
		Folder foldercheck = null;
		try {
			foldercheck = (Folder) session.getObjectByPath(fullpath + "/" + foldername);
		} catch (Exception e) {

		}

		String returnfolder = "";
		if (foldercheck == null) {
			Map<String, String> newFolderProps = new HashMap<String, String>();
			newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
			newFolderProps.put(PropertyIds.NAME, foldername);
			folder.createFolder(newFolderProps);

			returnfolder = folder.getId();
		} else {
			returnfolder = foldercheck.getId();
		}

		session.clear();

		return returnfolder;
	}

	boolean changeFolderAlfresco(String id, String oldfoldername, String newfoldername) throws Exception {
		Session session = createSession();

		Folder folder = (Folder) session.getObject(id);

		Map<String, String> newFolderProps = new HashMap<String, String>();
		newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
		newFolderProps.put(PropertyIds.NAME, newfoldername);
		folder.updateProperties(newFolderProps);

		session.clear();

		return true;
	}

	boolean deleteFolder(String id, boolean deleteSubFolder) throws Exception {
		Session session = createSession();

		Folder folder = (Folder) session.getObject(id);
		if (deleteSubFolder) {
			folder.deleteTree(true, null, true);
		} else {
			folder.delete();
		}

		return true;
	}

	public String getFolderIdFromPath(String path) throws Exception {
		Session session = createSession();
		Folder folder = (Folder) session.getObjectByPath(path);
		String id = folder.getId();

		session.clear();

		return id;
	}

	String getPathFromFolderId(String id) throws Exception {
		Session session = createSession();
		Folder folder = (Folder) session.getObject(id);
		List<String> paths = folder.getPaths();
		String path = paths.get(0);

		session.clear();

		return path;
	}

	String getUniqueFileName(Session session, Folder folder, String path, String original_filename, String filename,
			String ext, Integer count) {
		filename = filename.replaceAll(" ", "_");
		filename = filename.replaceAll("-", "_");
		ItemIterable<QueryResult> results = session.query("SELECT cmis:name FROM cmis:document where cmis:name='"
				+ filename + ext + "' and IN_FOLDER('" + folder.getId() + "')", false);
		if (results.getTotalNumItems() > 0) {
			filename = original_filename + "(" + count + ")";
			filename = getUniqueFileName(session, folder, path, original_filename, filename, ext, count + 1);
		}

		return filename;
	}

	public Folder connect(String repositoryId) {
		System.out.println(">> Connect");
		System.out.println("repo id: " + repositoryId);
		SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, "admin");
		parameter.put(SessionParameter.PASSWORD, "admin");
		parameter.put(SessionParameter.ATOMPUB_URL, AlfrescoUtil.ATOMPUB_URL);
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		parameter.put(SessionParameter.REPOSITORY_ID, repositoryId);

		Session session = sessionFactory.createSession(parameter);

		return session.getRootFolder();
	}

	/**
	 * 
	 * @param target
	 * @throws Exception 
	 */
	public ArrayList<ContentModel> listFolder(String folderId) throws Exception {

		int maxItemsPerPage = 10;
		ArrayList<ContentModel> contentModels = new ArrayList<>();

		Session session = createSession();
		System.out.println("Folder id: " + folderId);
		CmisObject object = session.getObject(session.createObjectId(folderId));
		Folder folder = (Folder) object;
		System.out.println("Folder name: " + folder.getName());
		OperationContext operationContext = session.createOperationContext();
		operationContext.setMaxItemsPerPage(maxItemsPerPage);

		ItemIterable<CmisObject> children = folder.getChildren(operationContext);

		Iterator<CmisObject> pageItems = children.iterator();
		
		while(pageItems.hasNext()) {
		    CmisObject item = pageItems.next();
		    ContentModel cm = new ContentModel();
		    cm.setId(item.getId());
		    cm.setName(item.getName());
		    
		    contentModels.add(cm);
		    
		    System.out.println(item.getName());
		}
		
		return contentModels;

	}

	public ArrayList<Document> queryByType(String queryType) throws Exception {
		ArrayList<Document> docs = new ArrayList<>();
		Session session = createSession();
		// get the query name of cmis:objectId
		ObjectType type = session.getTypeDefinition(queryType);
		PropertyDefinition<?> objectIdPropDef = type.getPropertyDefinitions().get(PropertyIds.OBJECT_ID);
		String objectIdQueryName = objectIdPropDef.getQueryName();

		String queryString = "SELECT " + objectIdQueryName + " FROM " + type.getQueryName();

		// execute query
		ItemIterable<QueryResult> results = session.query(queryString, false);

		for (QueryResult qResult : results) {
		   String objectId = qResult.getPropertyValueByQueryName(objectIdQueryName);
		   Document doc = (Document) session.getObject(session.createObjectId(objectId));
		   docs.add(doc);
		}
		
		return docs;
	}
	
}
