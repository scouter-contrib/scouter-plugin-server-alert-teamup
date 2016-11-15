package scouter.plugin.server.alert.teamup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.plugin.server.alert.teamup.pojo.Message;
import scouter.plugin.server.alert.teamup.pojo.OAuth2Token;
import scouter.plugin.server.alert.teamup.pojo.TeamUpAuth;
import scouter.plugin.server.alert.teamup.util.PatternsUtil;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.server.core.AgentManager;
import scouter.server.db.TextRD;
import scouter.util.DateUtil;

public class TeamUpPlugin {
	// Get singleton Configure instance from server
	final Configure conf = Configure.getInstance();
	final String GRANT_REFRESH = "refresh_token";
	final String GRANT_PASSWORD = "password";
	final String MESSAGE_URL = "https://edge.tmup.com/v3/message/";
	final String OAUTH2_URL = "https://auth.tmup.com/oauth2/token";
	
	private OAuth2Token oauth2Token;
	
	public TeamUpPlugin(){
		this.oauth2Token = getOauth2Token();
	}
	
	@ServerPlugin(PluginConstants.PLUGIN_SERVER_ALERT)
	public void alert(final AlertPack pack) {
		if (conf.getBoolean("ext_plugin_teamup_send_alert", true)) {
			println("[ext_plugin_teamup_send_alert true]");
			// Get log level (0 : INFO, 1 : WARN, 2 : ERROR, 3 : FATAL)
			int level = conf.getInt("ext_plugin_teamup_level", 0);

			if (level <= pack.level) {
				new Thread() {
					public void run() {
						try {
							String roomId = conf.getValue("ext_plugin_teamup_room_id");
							assert roomId != null;
							println("[roomId ok]");
							//get access token
							String token = getAccessToken();							
							if(token != null){
								// Get the agent Name
								String name = AgentManager.getAgentName(pack.objHash) == null ? "N/A"
										: AgentManager.getAgentName(pack.objHash);
	
								if (name.equals("N/A") && pack.message.endsWith("connected.")) {
									int idx = pack.message.indexOf("connected");
									if (pack.message.indexOf("reconnected") > -1) {
										name = pack.message.substring(0, idx - 6);
									} else {
										name = pack.message.substring(0, idx - 4);
									}
								}
	
								String title = pack.title;
								String msg = pack.message;
								if (title.equals("INACTIVE_OBJECT")) {
									title = "An object has been inactivated.";
									msg = pack.message.substring(0, pack.message.indexOf("OBJECT") - 1);
								}
	
								// Make message contents
								String contents = "[TYPE] : " + pack.objType.toUpperCase() + "\n" + "[NAME] : " + name
										+ "\n" + "[LEVEL] : " + AlertLevel.getName(pack.level) + "\n" + "[TITLE] : " + title
										+ "\n" + "[MESSAGE] : " + msg;
	
								Message message = new Message(contents);
								String param = new Gson().toJson(message);
								HttpPost post = new HttpPost(MESSAGE_URL + roomId);
								post.addHeader("Authorization", token);
								post.addHeader("Content-Type", "application/json");
								post.setEntity(new StringEntity(param));
	
								CloseableHttpClient client = HttpClientBuilder.create().build();
	
								// send teamup message
								HttpResponse response = client.execute(post);
								if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
									println("teamup message sent to [" + roomId + "] successfully.");
								} else {
									println("teamup message sent failed. Verify below information.");
									println("[URL] : " + MESSAGE_URL + roomId);
									println("[StatusCode] : " + response.getStatusLine().getStatusCode());
									println("[Message] : " + param);
									println("[Reason] : " + EntityUtils.toString(response.getEntity(), "UTF-8"));
									println("[AccessToken] : " + token);									
								}							
							}else{
								println("[Error] : token null");
							}
						} catch (Exception e) {
							println("[Error] : " + e.getMessage());
							if (conf._trace) {
								e.printStackTrace();
							}
						}
					}
				}.start();
			}
		}
	}

	@ServerPlugin(PluginConstants.PLUGIN_SERVER_OBJECT)
	public void object(ObjectPack pack) {
		if (pack.version != null && pack.version.length() > 0) {
			AlertPack ap = null;
			ObjectPack op = AgentManager.getAgent(pack.objHash);

			if (op == null && pack.wakeup == 0L) {
				// in case of new agent connected
				ap = new AlertPack();
				ap.level = AlertLevel.INFO;
				ap.objHash = pack.objHash;
				ap.title = "An object has been activated.";
				ap.message = pack.objName + " is connected.";
				ap.time = System.currentTimeMillis();
				ap.objType = "scouter";

				alert(ap);
			} else if (op.alive == false) {
				// in case of agent reconnected
				ap = new AlertPack();
				ap.level = AlertLevel.INFO;
				ap.objHash = pack.objHash;
				ap.title = "An object has been activated.";
				ap.message = pack.objName + " is reconnected.";
				ap.time = System.currentTimeMillis();
				ap.objType = "scouter";

				alert(ap);
			}
			// inactive state can be handled in alert() method.
		}
	}

	@ServerPlugin(PluginConstants.PLUGIN_SERVER_XLOG)
	public void xlog(XLogPack pack) {
		if (conf.getBoolean("ext_plugin_teamup_xlog_enabled", true)) {
			println("[ext_plugin_teamup_xlog_enabled true]");
			if (pack.error != 0) {
				String date = DateUtil.yyyymmdd(pack.endTime);
				String service = TextRD.getString(date, TextTypes.SERVICE, pack.service);
				String patterns = conf.getValue("ext_plugin_teamup_error_escape_method_patterns").length()>0?conf.getValue("ext_plugin_teamup_error_escape_method_patterns"):"*";
				if (!PatternsUtil.isValid(patterns, service)) {
					AlertPack ap = new AlertPack();
					ap.level = AlertLevel.ERROR;
					ap.objHash = pack.objHash;
					ap.title = "Ultron Error";
					ap.message = service + " - " + TextRD.getString(date, TextTypes.ERROR, pack.error);
					ap.time = System.currentTimeMillis();
					ap.objType = "scouter";
					alert(ap);
				}else{
					println("escape service : " + service);
				}
			}
		}
	}

	private void println(Object o) {
		if (conf.getBoolean("ext_plugin_teamup_debug", false)) {
			Logger.println(o);
		}
	}

	private String getAccessToken() {
		oauth2Token = getOauth2Token();
		return oauth2Token!=null?oauth2Token.getAccessToken():null;
	}
	
	private OAuth2Token getOauth2Token(){
		
		OAuth2Token resultToken = null;
		try{
			CloseableHttpClient client = HttpClientBuilder.create().build();
			if(oauth2Token!=null){
				if(oauth2Token.isExpired()){
					HttpGet get = new HttpGet(OAUTH2_URL + "?grant_type="+GRANT_REFRESH + "&refresh_token=" + oauth2Token.getRefreshToken());
					HttpResponse response = client.execute(get);		
					resultToken = new Gson().fromJson(EntityUtils.toString(response.getEntity()), OAuth2Token.class);
					println("[oauth2 refresh]");
					println("[oauth2 access token] " + resultToken.getAccessToken());
				}else{
					resultToken = oauth2Token;
				}
			}else{
				String client_id = conf.getValue("ext_plugin_teamup_bot_client_id");
				String client_secret = conf.getValue("ext_plugin_teamup_bot_client_secret");
				String username = conf.getValue("ext_plugin_teamup_bot_username");
				String password = conf.getValue("ext_plugin_teamup_bot_password");
				
				assert client_id != null;
				println("[client id ok]");
				assert client_secret != null;
				println("[client secret ok]");
				assert username != null;
				println("[username ok]");
				assert password != null;
				println("[password ok]");
				
				TeamUpAuth auth = new TeamUpAuth(GRANT_PASSWORD, client_id, client_secret, username, password);
				HttpPost post = new HttpPost(OAUTH2_URL);
				post.addHeader("Content-Type", "application/x-www-form-urlencoded");	
				
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		        JsonElement elm= new Gson().toJsonTree(auth);
		        JsonObject jsonObj=elm.getAsJsonObject();
		        for(Map.Entry<String, JsonElement> entry:jsonObj.entrySet()){
		            nameValuePairs.add(new BasicNameValuePair(entry.getKey(),entry.getValue().getAsString()));
		        }
		        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = client.execute(post);
				resultToken = new Gson().fromJson(EntityUtils.toString(response.getEntity()), OAuth2Token.class);
				println("[oauth2 created]");
				println("[oauth2 access token] " + resultToken.getAccessToken());
			}
		}catch(Exception e){
			println("[Error] : " + e.getMessage());
			if (conf._trace) {
				e.printStackTrace();
			}
		}
		
		return resultToken;
	}
}
