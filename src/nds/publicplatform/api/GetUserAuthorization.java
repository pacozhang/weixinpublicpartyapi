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
import nds.weixin.ext.WeUtils;
import nds.weixin.ext.WeUtilsManager;

public class GetUserAuthorization {
	private static Logger logger= LoggerManager.getInstance().getLogger(GetUserAuthorization.class.getName());
	private static String weixingetauthorizeruseropenid=WebUtils.getProperty("weixin.get_authorizer_user_openid","");
	private static String weixingetauthorizeruserinfo=WebUtils.getProperty("weixin.get_authorizer_user_info","");

	private static Hashtable<String,GetUserAuthorization> weixingetuserinfos;
	
	private WxPublicControl wpc;
	private WxPublicpartyControl wppc;
	
	private GetUserAuthorization(WxPublicControl wpc) {
		this.wppc=wpc.getPpc();
		this.wpc=wpc;
	}
	
	/**
	 * 
	 * @param pappid	公众号APPID
	 * @return
	 */
	public static synchronized GetUserAuthorization getInstance(String pappid) {
		GetUserAuthorization instance=null;
		if(weixingetuserinfos==null) {
			weixingetuserinfos=new Hashtable<String,GetUserAuthorization>();
			
			WxPublicControl twpc=null;

			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils wu=wum.getByAppid(pappid);
			twpc=WxPublicControl.getInstance(pappid);
			
			instance=new GetUserAuthorization(twpc);
			
			weixingetuserinfos.put(pappid, instance);
		}else if(weixingetuserinfos.containsKey(pappid)) {
			return weixingetuserinfos.get(pappid);
		}else {
			WxPublicControl twpc=null;

			WeUtilsManager wum=WeUtilsManager.getInstance();
			WeUtils wu=wum.getByAppid(pappid);
			twpc=WxPublicControl.getInstance(pappid);

			instance=new GetUserAuthorization(twpc);
			
			weixingetuserinfos.put(pappid, instance);
		}
		
		return instance;
	}

	/**
	 * 获取微信授权用户OPENID
	 * @param authorizedcode	微信用户授权码
	 * @return	微信授权用户 授权信息
	 */
	public JSONObject getAuthorizerUserOpenid(String authorizedcode) {
		JSONObject authorizedinfo=new JSONObject();
		
		WeUtils wu=wpc.getWxPublic();
		String pappid=wu.getAppId();
		String ppappid=wu.getPublicpartyappid();
		
		JSONObject atjo=wppc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				authorizedinfo.put("code", "-1");
				authorizedinfo.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return authorizedinfo;
		}
		
		String accesstoken=atjo.optJSONObject("data").optString("component_access_token");
		
		String url=weixingetauthorizeruseropenid+accesstoken;
		url=url.replace("$code$", authorizedcode);
		url=url.replace("$pappid$", pappid);
		url=url.replace("$ppappid$", ppappid);
		
		ValueHolder vh=null;
		
		logger.debug("get user authorizer user openid");
		
		try {
			vh=RestUtils.sendRequest_buff(url, "", "POST");
			String result=(String) vh.get("message");
			logger.debug("get user authorizer user openid result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			if(tjo!=null&&tjo.has("openid")) {
				authorizedinfo.put("code", 0);
				authorizedinfo.put("message", "获取成功");
				authorizedinfo.put("data", tjo);
			}else {
				authorizedinfo.put("code", -1);
				authorizedinfo.put("message", "获取用户授权信息失败");
			}
		}catch(Exception e){
			logger.debug("get user authorizer user openid error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				authorizedinfo.put("code", -1);
				authorizedinfo.put("message", "获取用户授权信息失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return authorizedinfo;
	}
	
	/***
	 * 
	 * @param jo	授权ACCESS TOKEN信息包括：公众号APPID,授权ACCESS TOKEN
	 * @return
	 */
	public JSONObject getAuthorizerUserinfo(JSONObject jo) {
		JSONObject userjo=new JSONObject();
		
		WeUtils wu=wpc.getWxPublic();
		String pappid=wu.getAppId();
		String ppappid=wu.getPublicpartyappid();
		
		
		/*
		JSONObject atjo=wppc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				userjo.put("code", "-1");
				userjo.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return userjo;
		}
		
		String accesstoken=atjo.optJSONObject("data").optString("component_access_token");
		*/
		
		String url=weixingetauthorizeruserinfo;
		url=url.replace("$at$", jo.optString("access_token"));
		url=url.replace("$openid$", jo.optString("openid"));
		
		ValueHolder vh=null;
		
		logger.debug("get user authorizer user info");
		
		try {
			vh=RestUtils.sendRequest_buff(url, "", "POST");
			String result=(String) vh.get("message");
			logger.debug("get user authorizer user info result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			if(tjo!=null&&tjo.has("openid")) {
				userjo.put("code", 0);
				userjo.put("message", "获取成功");
				userjo.put("data", tjo);
			}else {
				userjo.put("code", -1);
				userjo.put("message", "获取用户授权信息失败");
			}
		}catch(Exception e){
			logger.debug("get authorizer user infon error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				userjo.put("code", -1);
				userjo.put("message", "获取用户授权信息失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return userjo;
	}

	public JSONObject getAuthorizerUserinfo(String authorizedcode) {
		JSONObject userinfo=null;
		JSONObject authinfo=getAuthorizerUserOpenid(authorizedcode);
		
		userinfo=getAuthorizerUserinfo(authinfo);
		
		return userinfo;
	}
}
