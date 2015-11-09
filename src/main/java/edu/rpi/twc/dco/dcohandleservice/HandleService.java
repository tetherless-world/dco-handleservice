package edu.rpi.twc.dco.dcohandleservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.handle.hdllib.*;

import com.sun.jersey.spi.resource.Singleton;

/**
 * Handle Restful Service For DCOID service
 * @author cheny18@rpi.edu
 *
 */
@Produces("application/xml")
@Path("handles")
@Singleton
public class HandleService {

    // The private key is used by the handle server to verify the
    // use of the handle server
	File privateKeyFile;

    // The domain of the handle, everything before 11121.
	String domainPrefix;

    // Do we debug or not
	boolean debug;

    // The email account name we use to send emails. Since we're using rpi
    // email service this has to be an rpi email address
	String serviceEmailAccount;

    // The password for the email account to verify the account can send
    // email messages
	String serviceEmailPw;

    // The email account email address we use to send emails. Since
    // we're using rpi email service this has to be an rpi email address
	String serviceEmailAddress;

    // Since we have to use an RPI email address to send the emails we
    // want to set the reply to email address so that more then the one
    // rpi account gets any responses
	String replyEmailAddress;

	// people to send notices to when anything errors occurs
	ArrayList<String> adminEmails;
	
	// Passphrase for Handle 
	String passphrase;
	
	// IP filters
	ArrayList<String> IPFilters;
	/**
	 * HandleService constructor, statically sets variables.
     *
	 */
	public HandleService(){
        // FIXME: these variables need to be read in from a
        // configuration file so that they can be easily modified
        // without having to rebuild the services

		String result = "";
		Properties prop = new Properties();
		String propFileName = "config.properties";
 
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
		try {
			prop.load(inputStream);
			
		} catch (IOException e) {
			this.sendEmailRPI("[DCOID] Configuration file missing", "Expected config.properties");
		}
		
        // Location of the handle server private key
		privateKeyFile = new File(prop.getProperty("privateKeyFile"));
		//privateKeyFile = new File("/Users/cheny/Downloads/admpriv.bin");
		//  /usr/share/tomcat6/work/dcohandleservice/admpriv.bin
		
        // DCO domain prefix of all handles
		domainPrefix = prop.getProperty("domainPrefix");
		
		//Agent email account, password and address for the admin
		serviceEmailAccount = prop.getProperty("serviceEmailAccount");

        // This password is set prior to building the software
		serviceEmailPw = prop.getProperty("serviceEmailPw");

        // email address of the email account we use
		serviceEmailAddress = prop.getProperty("serviceEmailAddress");
		
		// Reply email address
		replyEmailAddress = prop.getProperty("replyEmailAddress");
		
		// Passphrase for interacting with 
		passphrase = prop.getProperty("passphrase");
		
		// Admin emails
		adminEmails = new ArrayList<String>(Arrays.asList(prop.getProperty("adminEmails").split(",")));
		
		// IP Filters
		IPFilters = new ArrayList<String>(Arrays.asList(prop.getProperty("IPFilters").split(",")));
		
		//Verbose message on/off
		debug = true;
	}
	
	/**
	 * Print regular info to standard out.
     *
     * Being a Apache Tomcat Webapp, this information is sent to the
     * tomcat log file catalina.out, line has prefix [INFO]
     *
	 * @param msg message to print
	 */
	public void printInfo(String msg){
		System.out.println("[INFO] "+msg);
	}

	/**
	 * Print error info to standard out.
     *
     * Being a Apache Tomcat Webapp, this information is sent to the
     * tomct log file catalina.out, line has prefix [ERROR]
     *
	 * @param msg message to print
	 */
	public void printError(String msg){
		System.out.println("[ERROR]"+msg);
	}
	
	private AuthenticationInfo getAuthInfo(){
		
		AuthenticationInfo myAuthInfo = null;
		
		try{
			byte[] userIDHandle=Util.encodeString("0.NA/11121");
	
			PrivateKey privateKey;
			
			privateKey = Util.getPrivateKeyFromFileWithPassphrase(privateKeyFile,passphrase);
			
	        // authenticate against the handle server
			myAuthInfo = new PublicKeyAuthenticationInfo(userIDHandle,300,privateKey);
			
		}catch(Exception e){
			this.sendEmailRPI("[DCOID] Failed to authenticate", "Please check passphrase and key file etc");
		}
		
		return myAuthInfo;
	}
	/**
	 * Mail authenticator used to authenticate the use of RPI email service.
     *
     * For the handle services, if we want to send emails regarding
     * error conditions, then we have to authenticate against the RPI
     * email services using an rpi account and rpi email address.
     *
	 * @author cheny
	 */
	class MailAuthenticator extends Authenticator {
         // RPI email services account user name
	     String user;
         // RPI email service account password
	     String pw;

         /**
          * MailAuthenticator constructor.
          *
          * @param username email account username
          * @param password email account password
          */
	     public MailAuthenticator (String username, String password)
	     {
	        super();
	        this.user = username;
	        this.pw = password;
	     }

        /**
         * grabs a new instance of PasswordAuthentication for the rpi service.
         *
         */
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	       return new PasswordAuthentication(user, pw);
	    }
	}
	
	/**
	 * Sending Email with RPI mail configurations.
     *
     * If there are issues with creating, modifying, retrieving,
     * removing a handle then we want to notify someone that there was a
     * problem.
     *
	 * @param title	Email Title
	 * @param body	Email Content
	 * @return String success or not
	 */
	public String sendEmailRPI(String title, String body){
        /*
         * The code is pretty straight forward. Create the email session
         * given addresses, create a new message setting the subject,
         * repy to address, and the body of the email. Then send.
         */
		String status = "";
		try{
			String host = "mail.rpi.edu";
		    Properties props = System.getProperties();
		    props.put("mail.smtp.starttls.enable", "true");
		    props.put("mail.smtp.host", host);
		    props.put("mail.smtp.port", "587");
		    props.put("mail.smtp.auth", "true");
		    props.put("mail.debug", debug);
		
		    Session session = Session.getInstance(props, new MailAuthenticator(this.serviceEmailAccount,this.serviceEmailPw));
		    MimeMessage message = new MimeMessage(session);
		    Address fromAddress = new InternetAddress(this.serviceEmailAddress);
		    Address[] toAddresses = new Address[this.adminEmails.size()];
		    for(int i=0;i<this.adminEmails.size();i++){
		    	toAddresses[i] = new InternetAddress(this.adminEmails.get(i));
 		    }
		    message.setFrom(fromAddress);
		    message.setRecipients(Message.RecipientType.TO, toAddresses);
		    message.setReplyTo(new InternetAddress[]{new InternetAddress(replyEmailAddress)});
		    message.setSubject(title);
		    message.setText(body);
		    Transport transport = session.getTransport("smtp");
		    transport.connect(host,587,this.serviceEmailAccount,this.serviceEmailPw);
		    message.saveChanges();
		    Transport.send(message,message.getAllRecipients());
		    transport.close();
		    status = "email done";
	    }catch(Exception ex){
	    	if(debug) printError("Sending email error:"+ex.toString());
	    	status = "email fail";
	    }finally{
	    	return status;
	    }
		
	}
	
	/**
	 * IP Address filter to make sure requests are coming from
     * particular IP address.
     *
     * We want to make sure that not just anyone can send requests to
     * this service. So we make sure the requests are coming from within
     * the RPI network
     *
	 * @param IPAddr incoming request IP address
	 * @return If the incoming IP address is valid or not
	 */
	public boolean checkIPAddress(String IPAddr){
        // FIXME: This should be part of the configuration file. An
        // array of acceptible IP addresses.
		printInfo(IPAddr);
		for(int i=0;i<IPFilters.size();i++){
			if(IPAddr.startsWith(IPFilters.get(i)))
				return true;
		}
		return false;
		
	}
	
	/**
	 * Supplementary function to create an XML file for response to
     * requests.
     *
     * Our responses look like the following:
     * <handle>
     *   <id>DCO-ID</id>
     *   <value>value being returned</value>
     * </handle>
     *
	 * @param handleID DCO-ID string
	 * @param handleValue 
	 * @return XML file
	 */
	public File createXML(String handleID,String handleValue){
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("handle");
			doc.appendChild(rootElement);
	 
			// handle id elements
			Element handleIDElement = doc.createElement("id");
			handleIDElement.appendChild(doc.createTextNode(handleID));
			rootElement.appendChild(handleIDElement);
			
			// handle id elements
			Element handleValueElement = doc.createElement("value");
			handleValueElement.appendChild(doc.createTextNode(handleValue));
			rootElement.appendChild(handleValueElement);
						
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File outputFile = new File("output.xml");
			StreamResult result = new StreamResult(outputFile);
	 
			// Output to console for testing
			transformer.transform(source, result);
			
			return outputFile;
			
		} catch (Exception e) {
			sendEmailRPI("[DCOHandleService] Creating XML response file failed","Failed to create XML file...");
			e.printStackTrace();
		}
 
		return null;
		
	}
	
	/**
	 * Generate a DCOID.
     *
     * for any of our services we generate a random DCOID to be assigned
     * to a DCO object. This is generally called from within the VIVO code.
     *
	 * @return String random DCOID
	 */
	public String handleGenerator(){
		
        // The DCO handle prefix
		String prefix = "11121/";
		
		String handle = prefix;
		int subHandle = 0;
		String subHandleString = "";
		
        // we add to the handle 4 sets of 4 randomly generated digits.
        // Then we tack on a -CC at the end
		for(int i=0;i<4;i++){
			
			while((subHandle=(int) (Math.random()*10000))<=1000);
			
			subHandleString = Integer.toString(subHandle);
			handle = handle + subHandleString;
			
			if(i!=3)
				handle = handle + "-";
			else
				handle = handle + "-CC";
			
		}
		return handle;
	}
	
	/**
	 * Resolve a handle to resource.
     *
     * Given a DCO-ID we look up that ID and return the resolvable URL
     *
     * @param requestedHandle the handle object that contains information
     * about the handle we're interested in
     * @param req the HTTP request object used to retrieve this
     * information
	 * @return Handles being resolved
	 */
	@Produces("application/xml")
	@Consumes("application/xml")
	@POST
	@Path("resolve")
	public Vector<handle> resolveHandle(handle requestedHandle,@Context HttpServletRequest req) {
		Vector<handle> allRecords = new Vector<handle>();

        // Make sure the request is coming from the approved list of IP
        // addresses. If not then return the empty vector
		if(!checkIPAddress(req.getRemoteHost())){
			return allRecords;
		}
		
        String requestHandleString = requestedHandle.getId().replace('.', '/');
        if(debug) printInfo("The requested handle is "+requestHandleString);
		try{
            // getHandle pulls back the information for the handle
			HandleValue[] indicator = this.getHandle(requestHandleString);
			
            // set the information in a new handle object and add it
            // to the return list
			for(int i = 0; i < indicator.length; i++){
				handle myHandle = new handle();
				myHandle.setId(requestHandleString);
				myHandle.setValue(indicator[i].getDataAsString());
				allRecords.add(myHandle);
			}
			
		}catch(Exception e){
			sendEmailRPI("[DCOHandleSerivce] Error in resolving handle" + requestHandleString, e.toString());
		}
		
		return allRecords;
	}
	
	/**
	 * Obtain all handle records and the information about them.
     *
     * This function could be used to retrieve all the handles, all the
     * information about them, and display them on an administrative
     * page.
     *
     * The response could get very, very large. As of 20141009 there are
     * 12,000 valid handles, 24,000 total handles
     *
	 * @return A vector of all the handles
	 */
	@GET
	@Path("allHandles")
	@Produces("application/xml")
	public Vector<handle> getAllHandles() {
		
        // the return vector of all the handles
		Vector<handle> allHandles = new Vector<handle>();
        // authentication information to the handle server
		byte[] userIDHandle=Util.encodeString("0.NA/11121");

		try {			
			
			ListHandlesRequest request = new ListHandlesRequest(userIDHandle,this.getAuthInfo());
			
			HandleResolver resolver = new HandleResolver();	
			
			messageCallBackImpl allHandleCallBack = new messageCallBackImpl();
			AbstractResponse response = resolver.processRequest(request,allHandleCallBack);
			
		} catch (Exception ex) {
			// TODO Auto-generated catch-block stub.
			ex.printStackTrace();
		}
		
		return allHandles;
	}
	
	/**
	 * Call back class for handling async response.
     *
     * Requests to the handle server use a callback function to handle
     * asynchronous requests.
     *
	 * @author cheny
	 *
	 */
	class messageCallBackImpl implements ResponseMessageCallback{

		public void handleResponse(AbstractResponse arg0)
				throws HandleException {
			//This function handles the continuous response after sending post request
			if(debug) printInfo("Response bounce back");
			if(debug) printInfo(arg0.messageBody.toString());
		}
		
	}
	
	/**
	 * Public function to generate a new DCO-ID for a resource.
     *
     * Now values are yet added to the handle. That is done in a
     * separate public function
     *
	 * @param myHandle XML object of requested handle
	 * @return XML object of requested handle with allocated DCO-ID;Null if error happens
	 */
	@POST
	@Path("create")
	@Produces("application/xml")
	@Consumes("application/xml")
	public File createHandle(handle myHandle,@Context HttpServletRequest req){
		
		File handleFile = null;

        // Make sure the request is made from an approved IP address
		if(!checkIPAddress(req.getRemoteHost())){
			return handleFile;
		}

		try{
			// Generate handle ID
			String handleID = this.handleGenerator();
			
			// Register hanlde ID with value in myHandle
			String indicator = this.registerHandle(handleID, myHandle.getValue() ,myHandle.getValue());
			if(indicator=="failed"){
				return this.createXML("NULL", "NULL");
			}
			//Encapsulate the results into xml
			if(debug) printInfo("handleID "+handleID+" handleValue "+myHandle.getValue());
			handleFile = this.createXML(domainPrefix+handleID, myHandle.getValue());
		}catch(Exception e){
			sendEmailRPI("[DCOHandleSerivce]Create DCOID Error", e.toString());
		}
		
		return handleFile;
		
	}
	
	/**
	 * Actual function to register a DCO-ID, will be shared by DCO-ID
     * creation and update.
     *
	 * @param handleID requested DCO-ID string
	 * @param handleType type of the resource
	 * @param handleValue value to be requested
	 * @return String message to show if the DCO-ID has been created successfully
	 */
	public String registerHandle(String handleID,String handleType,String handleValue){
		
        // the handle being registered
		byte[] myHandle=Util.encodeString(handleID);
		
        // the URL the handle resolves to is at index 1
		int objectIndex = 1;
        // the type of the URL is of course URL
		byte objectType[] = Util.encodeString("URL");
        // the value of this is the URL the handle resolves to. When
        // creating a new handle this ends up being empty and must be
        // updated in a later call
		byte objectData[] = Util.encodeString(handleValue);
		HandleValue myResourceObject = new HandleValue(objectIndex,objectType,objectData);
		HandleValue[] myResourceObjectList = {myResourceObject};
		
		try {
			
            // build the request object that gets passed to the handle
            // server. This contains just the one item; the handle, the
            // type URL, and the URL the handle resolves to.
			CreateHandleRequest request = new CreateHandleRequest(myHandle,myResourceObjectList,this.getAuthInfo());
			if(request==null){
				sendEmailRPI("[DCOHandleService] Failed to register the handle " + handleID, "Register Handle Error");
				return "failed";
			}
			HandleResolver resolver = new HandleResolver();
			
            // make the request to the handle server
			AbstractResponse response = resolver.processRequest(request);
			if(debug) printInfo("The response code is "+response.responseCode+" While success is "+AbstractMessage.RC_SUCCESS);
			// if the response value matches the enum for success, then return success. 
			if(response.responseCode==AbstractMessage.RC_SUCCESS){
				if(debug) printInfo("Registry successful");
				return "handle has been successfully created"+"With key"+handleID+" and value "+handleValue;
			}else{
				sendEmailRPI("[DCOHandleService] Failed to register the handle " + handleID, "Register Handle Error");
				return "failed";
			}
			
		} catch (Exception exception1) {
			// send email to admins if anything happens. 
			sendEmailRPI("[DCOHandleService] Failed to register the handle " + handleID, "Register Handle Error");
			exception1.printStackTrace();
		}
		
		return "failed";
	}
	
	
	
	/**
	 * Actual function to resolve a DCO-ID.
     *
     * This function makes the call to the handle server to retrieve the
     * information about a given handle. Since this is a simple get
     * request against the handle we do not need to do any
     * authentication to the handle server.
     *
	 * @param handleID DCO-ID to be resolved
	 * @return resource value for the DCO-ID
	 */
	public HandleValue[] getHandle(String handleID){
		byte myHandle[]=Util.encodeString(handleID);
		ResolutionRequest request = new ResolutionRequest(myHandle,null,null,null);
		HandleResolver resolver = new HandleResolver();
		String handleRecord = "";
		HandleValue[] values = null;
		try {
			AbstractResponse response = resolver.processRequest(request);
			if(response.responseCode==AbstractMessage.RC_SUCCESS){
				
				 values=((ResolutionResponse)response).getHandleValues();
				for(int i=0;i<values.length;i++){
					if(debug) System.out.println(String.valueOf(values[i]));
					handleRecord = handleRecord + String.valueOf(values[i]);
				}
				
				return values;
			}
			
		} catch (HandleException exception) {
			// TODO Auto-generated catch-block stub.
			this.sendEmailRPI("[DCOHandleService] Resolve Handle error", exception.toString());
			exception.printStackTrace();
		}
		
		return values;
	}

	
	/**
	 * Modify a resource with a just created DCO-ID.
     *
     * For DCO purposes we modify the handle with a new URL
     *
	 * @param newHandle Handle XML object
	 * @return String success or not
	 * @throws HandleException
	 */
	@POST
	@Path("modifyurl")
	@Produces("application/xml")
	@Consumes("application/xml")
	public String modifyRedirectURL(handle newHandle,@Context HttpServletRequest req){
		
		String status = "failed";

        // Make sure the request is coming from a valid IP address
		if(!checkIPAddress(req.getRemoteHost())){
			return "failed";
		}
		
		try{
			
            // encode the handle
			byte[] myHandleID=Util.encodeString(newHandle.getId());

            // The value we're interested in is at index 1, the URL the
            // handle resolves to
			int objectIndex = 1;
            
            // Fill in the type of the value, which is URL
			byte objectType[] = Util.encodeString(newHandle.getType());

            // Fill in the value, the URL the handle resolves to
			byte objectData[] = Util.encodeString(newHandle.getValue());

            // Given the above information, fill in the handle
            // information we're updating
			HandleValue myResourceObject = new HandleValue(objectIndex,objectType,objectData);
			
			HandleValue[] myResourceObjectList = {myResourceObject};
			
            // Build the request that will be sent to the handle server
			ModifyValueRequest updateHandleRequest = new ModifyValueRequest(myHandleID,myResourceObjectList,this.getAuthInfo());
			
			HandleResolver resolver = new HandleResolver();
			
            // make the request to the handle server
			AbstractResponse response = resolver.processRequest(updateHandleRequest);
			if(debug) printInfo("The response code is "+response.responseCode+" While success is "+AbstractMessage.RC_SUCCESS);
			if(response.responseCode==AbstractMessage.RC_SUCCESS){
				status = "successful";
			}else{
				this.sendEmailRPI("[DCOHandleService] Modify DCOID error", "Response code is"+response.responseCode);
			}
		}catch(Exception e){
			sendEmailRPI("[DCOHandleSerivce] Modify DCOID error", e.toString());
		}
		return status;
		
	}
	
	/**
	 * Create the DCO-ID with a new resource if DCO-ID exists;otherwise
     * just create a new one.
     *
     * The DCO ID has been created and has a redirect URL. This function
     * modifies that handle with a new URL
     *
     * @param newHandle handle object with the new information
     * @param req http request object to pull information from
	 * @return successful or not
	 * @throws HandleException 
	 */
	@Path("update")
	@POST
	@Produces("application/xml")
	@Consumes("application/xml")
	public String updateHandle(handle newHandle,@Context HttpServletRequest req) throws HandleException {
		
		String status = null;

        // Make sure the request is coming from a valid IP address
		if(!checkIPAddress(req.getRemoteHost())){
			return status;
		}
		
		try{
			if(debug) printInfo("The new id is "+newHandle.getId()+" And the new value is "+newHandle.getValue());
						
			// Find if there is previously existed handle
			HandleValue[] handleValue = this.getHandle(newHandle.getId());
			
            // If the handle doesn't exist then just create a new one
			if(handleValue==null){
				status = this.registerHandle(newHandle.getId(),newHandle.getType(), newHandle.getValue());
			}else{
				status = this.modifyRedirectURL(newHandle, req);
			}
		}catch(Exception e){
			sendEmailRPI("[DCOHandleService] update DCOID error", e.toString());
		}
		return status;
		
	}

	/**
	 * Remove a DCO-ID.
     *
     * @param handleRecord the handle to be removed
     * @param req the http request information for this request
	 * @return successful or not
	 */
	@POST
	@Path("remove")
	@Consumes("application/xml")
	@Produces("application/xml")
	public String removeHandle(handle handleRecord,@Context HttpServletRequest req) {
		
		String success = "done"; 

        // Make sure the request is coming from a valid IP address
		if(!checkIPAddress(req.getRemoteHost())){
			return "failed";
		}
		try{
            // encode the handle that we're interested in removing
			byte[] myHandleID=Util.encodeString(handleRecord.getId());

            // build the request object
			DeleteHandleRequest deleteRequest = new DeleteHandleRequest(myHandleID,this.getAuthInfo());
			HandleResolver resolver = new HandleResolver();

            // make the request
			AbstractResponse response = null;
			try {
				response = resolver.processRequest(deleteRequest);
			} catch (HandleException e) {
				sendEmailRPI("[DCOHandleService] Delete request error",e.toString());
				e.printStackTrace();
			}
			if(debug) printInfo("The response code is "+response.responseCode+" While success is "+AbstractMessage.RC_SUCCESS);
			if(response.responseCode==AbstractMessage.RC_SUCCESS){
				success = "done";
			}else{
				sendEmailRPI("[DCOHandleService] failed to remove a dcoid","failed to remove"+handleRecord.getId());
				success = "failed";
			}
		}catch(Exception e){
			sendEmailRPI("[DCOHandleService] Remove DCOID", e.toString());
		}
		
		return success;
	}
}
