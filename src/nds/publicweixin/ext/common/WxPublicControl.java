package nds.publicweixin.ext.common;

import java.util.Hashtable;

import org.json.JSONObject;

import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicplatform.api.RefreshAccessToken;
import nds.weixin.ext.WePublicparty;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

public class WxPublicControl {
	private static Logger logger= LoggerManager.getInstance().getLogger(WxPublicControl.class.getName());
	
	private final WeUtils wu;
	private final WxPublicpartyControl wppc;
	private static Hashtable<String,WxPublicControl> wpcs;
	private WxPublicControl(WxPublicpartyControl wppc,WeUtils wu) {
		this.wu=wu;
		this.wppc=wppc;
	}
	
	/**
	 * 
	 * @param pappid	public	APPID(公众号APPID)
	 * @return
	 */
	public static synchronized WxPublicControl getInstance(String pappid) {
		logger.debug("pappid is->"+pappid);
		WxPublicControl twpc=null;
		WxPublicpartyControl twppc=null;
		if(wpcs==null) {
			wpcs=new Hashtable<String,WxPublicControl>();
			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils twu=wum.getByAppid(pappid);
			twppc=WxPublicpartyControl.getInstance(twu.getPublicpartyappid());
			twpc=new WxPublicControl(twppc,twu);
			wpcs.put(pappid, twpc);
			return twpc;
		}else if(wpcs.containsKey(pappid)) {
			return wpcs.get(pappid);
		}else {
			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils twu=wum.getByAppid(pappid);
			twppc=WxPublicpartyControl.getInstance(twu.getPublicpartyappid());
			twpc=new WxPublicControl(twppc,twu);
			wpcs.put(pappid, twpc);
			return twpc;
		}
	}
	
	public JSONObject getAccessToken() {
		JSONObject atoken=new JSONObject();
		if(nds.util.Validator.isNull(wu.getAuthorizer_access_token())||wu.getAattime()<System.currentTimeMillis()) {
			RefreshAccessToken rat=RefreshAccessToken.getInstance(this);
			atoken=rat.refreshAccessToken();
			JSONObject jo=null;
			
			if(atoken!=null&&"0".equals(atoken.optString("code"))) {
				jo=atoken.optJSONObject("data");
				if(jo!=null&&jo.has("authorizer_access_token")) {wu.setAuthorizerinfo(jo);}
			}else{
				try{
					atoken.put("code", "-1");
					atoken.put("message", (jo!=null?jo.optString("message","失败"):"失败"));
				}catch(Exception e){
					
				}
			}
		}else{
			try{
				atoken.put("code", "0");
				atoken.put("message", "成功");
				atoken.put("data", new JSONObject().put("authorizer_access_token", wu.getAuthorizer_access_token()));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		
		return atoken;
		//return wu.getAuthorizer_access_token();
	}

	public JSONObject getJssdk_access_token(){
		JSONObject jstoken=new JSONObject();
		if(nds.util.Validator.isNull(wu.getAuthorizer_access_token())||wu.getJssdktime()<System.currentTimeMillis()){
			RefreshAccessToken rat=RefreshAccessToken.getInstance(this);
			jstoken=rat.getJssdkAccessToken();
			
			logger.debug("getJssdk_access_token:"+jstoken);
			if(jstoken!=null&&"0".equals(jstoken.optString("code"))) {
				JSONObject jo=jstoken.optJSONObject("data");
				if(jo!=null&&jo.has("ticket")) {wu.setJssdk_access_token(jo);}
			}
		}else{
			try{
				jstoken.put("code", "0");
				jstoken.put("message", "成功");
				jstoken.put("data", new JSONObject().put("ticket", wu.getJssdk_access_token()));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return jstoken;
	}
	
	/*
	public String getAccessToken() {
		if(nds.util.Validator.isNull(wu.getAuthorizer_access_token())||wu.getAattime()<System.currentTimeMillis()) {
			RefreshAccessToken rat=RefreshAccessToken.getInstance(this);
			JSONObject jo=rat.refreshAccessToken();
			
			if(jo!=null&&"0".equals(jo.optString("code"))) {
				jo=jo.optJSONObject("data");
				if(jo!=null&&jo.has("authorizer_access_token")) {wu.setAuthorizerinfo(jo);}
			}
		}
		
		return wu.getAuthorizer_access_token();
	}
	*/
	
	public WeUtils getWxPublic() {
		return this.wu;
	}

	public WxPublicpartyControl getPpc() {
		return this.wppc;
	}
}
