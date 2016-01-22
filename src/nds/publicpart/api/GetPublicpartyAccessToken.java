package nds.publicpart.api;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicpartyControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.weixin.ext.WePublicparty;

public class GetPublicpartyAccessToken {
	private static Logger logger= LoggerManager.getInstance().getLogger(GetPublicpartyAccessToken.class.getName());
	private static String weixingetpublicpartyaccesstoken=WebUtils.getProperty("weixin.get_publicparty_access_token_URL","");
	
	//获取第三方平台accesstoken		//synchronized
	public JSONObject getAccessToken(WxPublicpartyControl wppc) {
		WePublicparty wpp=wppc.getWePublicparty();
		JSONObject accesstokeninfo=new JSONObject();
		JSONObject param=new JSONObject();
		if(wpp==null||nds.util.Validator.isNull(wpp.getAppid()) ||nds.util.Validator.isNull(wpp.getAppsecret())) {
			logger.debug("WePublicparty error->");
			return accesstokeninfo;
		}
		
		
		try {
			param.put("component_appid", wpp.getAppid());
			param.put("component_appsecret", wpp.getAppsecret());
			param.put("component_verify_ticket", wpp.getComponent_verify_ticket());
		}catch(Exception e){
			
		}
		
		ValueHolder vh=null;
		logger.debug("get publicparty access token");
		
		try {
			vh=RestUtils.sendRequest_buff(weixingetpublicpartyaccesstoken, param.toString(), "POST");
			String result=(String) vh.get("message");
			logger.debug("get publicparty access token result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			String returns="获取授权码失败！";
			if(tjo!=null&&tjo.has("component_access_token")) {
				
				accesstokeninfo.put("code", 0);
				accesstokeninfo.put("message", "获取成功");
				accesstokeninfo.put("data", tjo);
			}else {
				accesstokeninfo.put("code", -1);
				accesstokeninfo.put("message", returns);
			}
		}catch(Exception e){
			logger.debug("get publicparty access token error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				accesstokeninfo.put("code", -1);
				accesstokeninfo.put("message", "获取授权码失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return accesstokeninfo;
	}
}
