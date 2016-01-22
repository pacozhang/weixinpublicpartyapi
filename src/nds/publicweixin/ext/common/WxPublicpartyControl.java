package nds.publicweixin.ext.common;

import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicpart.api.GetPreAuthorizerCode;
import nds.publicpart.api.GetPublicpartyAccessToken;
import nds.weixin.ext.WePublicparty;
import nds.weixin.ext.WePublicpartyManger;

public class WxPublicpartyControl {
	private static Hashtable<String,WxPublicpartyControl> wppcs;
	
	private static Logger logger= LoggerManager.getInstance().getLogger(WxPublicpartyControl.class.getName());
	
	private final WePublicparty wpp;
	
	private WxPublicpartyControl(WePublicparty wpp) {
		this.wpp=wpp;
	}
	
	/**
	 * 
	 * @param ppappid	第三方应用APPID
	 * @return
	 */
	public static synchronized WxPublicpartyControl getInstance(String ppappid) {
		logger.debug("ppappid is->"+ppappid);
		WxPublicpartyControl wppc=null;
		
		WePublicpartyManger wppm=WePublicpartyManger.getInstance();
		if(wppcs==null) {
			wppcs=new Hashtable<String,WxPublicpartyControl>();
			WePublicparty wpp=wppm.getWpc();//wppm.getByAppid(ppappid);
			
			wppc=new WxPublicpartyControl(wpp);
			wppcs.put(wpp.getAppid(), wppc);
		}
		else if(wppcs.containsKey(ppappid)) {return wppcs.get(ppappid);}
		else {
			WePublicparty wpp=wppm.getWpc();//wppm.getByAppid(ppappid);
			wppc=new WxPublicpartyControl(wpp);
			wppcs.put(wpp.getAppid(), wppc);
		}
		
		return wppc;
	}

	public JSONObject getAccessToken() {
		JSONObject atjo=new JSONObject();
		if(nds.util.Validator.isNull(wpp.getComponent_access_token())||wpp.getCattime()<System.currentTimeMillis()) {
			GetPublicpartyAccessToken gat=new GetPublicpartyAccessToken();
			atjo=gat.getAccessToken(this);
			
			if(atjo!=null&&"0".equals(atjo.optString("code"))) {
				JSONObject jo=atjo.optJSONObject("data");
				if(jo!=null&&jo.has("component_access_token")) {
					wpp.setComponent_access_token(jo);
				}
			}
			return atjo;
		}
		
		try {
			atjo.put("code", "0");
			atjo.put("message", "success");
			atjo.put("data", new JSONObject().put("component_access_token", wpp.getComponent_access_token()));
		} catch (JSONException e) {
			logger.error("init getaccesstoken error:"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		return atjo;
	}
	
	public JSONObject getPreAccessToken() {
		JSONObject patjo=new JSONObject();
		logger.debug("wpp.getPre_authorized_token:"+wpp.getPre_authorized_token()+",wpp.getPattime():"+wpp.getPattime()+",System.currentTimeMillis()"+System.currentTimeMillis());
		if(nds.util.Validator.isNull(wpp.getPre_authorized_token())||wpp.getPattime()<System.currentTimeMillis()) {
			GetPreAuthorizerCode gpat=new GetPreAuthorizerCode();
			patjo=gpat.getPreAuthorizerCode(this);
			
			logger.debug("get accesstoken result->"+patjo);
			if(patjo!=null&&"0".equals(patjo.optString("code"))) {
				JSONObject jo=patjo.optJSONObject("data");
				if(jo!=null&&jo.has("pre_auth_code")) {
					wpp.setPre_authorized_token(jo);
				}
			}
			return patjo;
		}
		
		try {
			patjo.put("code", "0");
			patjo.put("message", "success");
			patjo.put("data", new JSONObject().put("pre_auth_code", wpp.getPre_authorized_token()));
		} catch (JSONException e) {
			logger.error("init getpreaccesstoken error:"+e.getLocalizedMessage());
			e.printStackTrace();
		}
		return patjo;
	}

	public WePublicparty getWePublicparty() {
		return this.wpp;
	}
}