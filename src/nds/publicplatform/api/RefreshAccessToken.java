package nds.publicplatform.api;

import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.common.WxPublicpartyControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.weixin.ext.WePublicparty;
import nds.weixin.ext.WePublicpartyManger;
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

public class RefreshAccessToken {
	private static Logger logger= LoggerManager.getInstance().getLogger(RefreshAccessToken.class.getName());
	private static String weixinrefreshpublicaccesstoken=WebUtils.getProperty("weixin.refresh_public_access_token_URL","");
	private static final String weixingetjssdkaccesstoken=WebUtils.getProperty("weixin.jssdk_access_token_URL","https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=%s&type=jsapi");
	
	private static Hashtable<String,RefreshAccessToken> weixinrefauths;
	
	private WxPublicControl wpc;
	private WxPublicpartyControl wppc;
	private RefreshAccessToken(WxPublicControl wpc) {
		this.wpc=wpc;
		this.wppc=wpc.getPpc();
	}
	
	//获取刷新授权实例，每个公众号APPID一个处理实例
	public static synchronized RefreshAccessToken getInstance(WxPublicControl wpc) {
		RefreshAccessToken instance=null;
		
		String pappid=wpc.getWxPublic().getAppId();
		if(weixinrefauths==null) {
			weixinrefauths=new Hashtable<String,RefreshAccessToken>();
			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils wu=wum.getByAppid(pappid);
			instance=new RefreshAccessToken(wpc);
			weixinrefauths.put(pappid, instance);
		}else if(weixinrefauths.containsKey(pappid)) {
			instance=weixinrefauths.get(pappid);
		}else {
			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils wu=wum.getByAppid(pappid);
			instance=new RefreshAccessToken(wpc);
			weixinrefauths.put(pappid, instance);
		}
		
		return instance;
	}

	
	public JSONObject refreshAccessToken() {
		JSONObject reauthorinfo=new JSONObject();
		
		WeUtils wu=wpc.getWxPublic();
		if(nds.util.Validator.isNull(wu.getAuthorizer_refresh_token())) {
			try {
				reauthorinfo.put("code", "-1");
				reauthorinfo.put("message", "请用户重新授权");
			}catch(Exception e){
				
			}
			
			return reauthorinfo;
		}
		
		WePublicparty wpp=wppc.getWePublicparty();
		
		JSONObject param=new JSONObject();
		try {
			param.put("component_appid", wpp.getAppid());
			param.put("authorizer_appid", wu.getAppId());
			param.put("authorizer_refresh_token", wu.getAuthorizer_refresh_token());
		}catch(Exception e){
			
		}
		
		JSONObject atjo=wppc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				reauthorinfo.put("code", "-1");
				reauthorinfo.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return reauthorinfo;
		}
		
		String accesstoken=atjo.optJSONObject("data").optString("component_access_token");
		
		ValueHolder vh=null;
		String url=weixinrefreshpublicaccesstoken+accesstoken;
		logger.debug("refresh public authorization code token");
		
		try {
			vh=RestUtils.sendRequest_buff(url, param.toString(), "POST");
			String result=(String) vh.get("message");
			logger.debug("refresh public authorized access token result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			if(tjo!=null&&tjo.has("authorizer_access_token")) {
				reauthorinfo.put("code", "0");
				reauthorinfo.put("message", "获取成功");
				reauthorinfo.put("data", tjo);
			}else {
				reauthorinfo.put("code", "-1");
				reauthorinfo.put("message","刷新授权码失败！");
			}
		}catch(Exception e){
			logger.debug("refresh public authorization access token error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				reauthorinfo.put("code", "-1");
				reauthorinfo.put("message", "刷新授权码失败！");
			} catch (JSONException e1) {
				
			}
		}
		return reauthorinfo;
	}

	public JSONObject getJssdkAccessToken(){
		JSONObject jsat=new JSONObject();
		
		WeUtils wu=wpc.getWxPublic();
		if(nds.util.Validator.isNull(wu.getAuthorizer_refresh_token())) {
			try {
				jsat.put("code", "-1");
				jsat.put("message", "请用户重新授权");
			}catch(Exception e){
				
			}
			
			return jsat;
		}
		

		JSONObject atjo=wpc.getAccessToken();
		
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				jsat.put("code", "-1");
				jsat.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return jsat;
		}
		
		ValueHolder vh=null;
		String accesstoken=atjo.optJSONObject("data").optString("authorizer_access_token");
		
		String url=String.format(weixingetjssdkaccesstoken, accesstoken);
		logger.debug("get jssdk access token");
		
		try {
			vh=RestUtils.sendRequest_buff(url, "", "POST");
			String result=(String) vh.get("message");
			logger.debug("get jssdk access token result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			if(tjo!=null&&tjo.has("ticket")) {
				jsat.put("code", "0");
				jsat.put("message", "获取成功");
				jsat.put("data", tjo);
			}else {
				jsat.put("code", "-1");
				jsat.put("message","获取JSSDKTICKET失败！");
			}
		}catch(Exception e){
			logger.debug("get jssdk access token error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				jsat.put("code", "-1");
				jsat.put("message", "获取JSSDKTICKET失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return jsat;
	}
}
