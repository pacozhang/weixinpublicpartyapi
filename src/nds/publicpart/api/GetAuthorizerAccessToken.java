package nds.publicpart.api;

import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicpartyControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.weixin.ext.WePublicparty;
import nds.weixin.ext.WePublicpartyManger;

/**
 * 获取授权公众号授权信息
 * @author paco
 *
 */
public class GetAuthorizerAccessToken {
	private static Logger logger= LoggerManager.getInstance().getLogger(GetAuthorizerAccessToken.class.getName());
	private static String weixingetauthorizedaccesstoken=WebUtils.getProperty("weixin.get_authorized_access_token_URL","");

	private static Hashtable<String,GetAuthorizerAccessToken> weixinauths;
	
	private WxPublicpartyControl wppc;
	private GetAuthorizerAccessToken(WxPublicpartyControl wppc) {
		this.wppc=wppc;
	}
	
	/**
	 * 获取公众号授权实例，每个APPID一个处理实例
	 * @param appid 应用APPID
	 * @return
	 */
	public static synchronized GetAuthorizerAccessToken getInstance(String appid) {
		GetAuthorizerAccessToken instance=null;
		
		
		if(weixinauths==null) {
			weixinauths=new Hashtable<String,GetAuthorizerAccessToken>();
			
			WxPublicpartyControl twppc=WxPublicpartyControl.getInstance(appid);
			instance=new GetAuthorizerAccessToken(twppc);
			weixinauths.put(appid, instance);
		}else if(weixinauths.containsKey(appid)) {
			instance=weixinauths.get(appid);
		}else {
			WxPublicpartyControl twppc=WxPublicpartyControl.getInstance(appid);
			instance=new GetAuthorizerAccessToken(twppc);
			weixinauths.put(appid, instance);
		}
		
		
		return instance;
	}
	
	
	//获取公众号accesstoken
	/**
	 * 获取授权公众号授权信息
	 * @param authorizercode 公众号授权码
	 * @return
	 */
	public JSONObject getAuthAccessToken(String authorizercode) {
		JSONObject authorizeraccesstokeninfo=new JSONObject();
		WePublicparty wpp=wppc.getWePublicparty();
		
		JSONObject param=new JSONObject();
		try {
			param.put("component_appid", wpp.getAppid());
			param.put("authorization_code", authorizercode);
		}catch(Exception e){
			
		}
		
		
		JSONObject atjo=wppc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				authorizeraccesstokeninfo.put("code", "-1");
				authorizeraccesstokeninfo.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return authorizeraccesstokeninfo;
		}
		
		String accesstoken=atjo.optJSONObject("data").optString("component_access_token");

		ValueHolder vh=null;
		String url=weixingetauthorizedaccesstoken+accesstoken;
		logger.debug("get public authorization info");
		
		try {
			vh=RestUtils.sendRequest_buff(url, param.toString(), "POST");
			String result=(String) vh.get("message");
			logger.debug("get public authorization info result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			String returns="获预取授权码失败！";
			if(tjo!=null&&tjo.has("authorization_info")) {
				authorizeraccesstokeninfo.put("code", 0);
				authorizeraccesstokeninfo.put("message", "获取成功");
				authorizeraccesstokeninfo.put("data", tjo);
			}else {
				authorizeraccesstokeninfo.put("code", -1);
				authorizeraccesstokeninfo.put("message", returns);
			}
		}catch(Exception e){
			logger.debug("get public authorization info error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				authorizeraccesstokeninfo.put("code", -1);
				authorizeraccesstokeninfo.put("message", "获取授权码失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return authorizeraccesstokeninfo;
	}
}//sun