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

public class GetPreAuthorizerCode {
	private static Logger logger= LoggerManager.getInstance().getLogger(GetPreAuthorizerCode.class.getName());
	private static String weixingetpreauthorizercode=WebUtils.getProperty("weixin.get_pre_authorizer_code_URL","");

	private static Hashtable<String,GetAuthorizerAccessToken> weixinauths;
	
	private WxPublicpartyControl wppc;
	
	//获取预授权码
	public JSONObject getPreAuthorizerCode(WxPublicpartyControl wppc) {
		JSONObject preauthcodeinfo =new JSONObject();
		
		JSONObject param=new JSONObject();
		try {
			param.put("component_appid", wppc.getWePublicparty().getAppid());
		}catch(Exception e){
			
		}
		
		ValueHolder vh=null;
		JSONObject atjo=wppc.getAccessToken();
		
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			try {
				preauthcodeinfo.put("code", "-1");
				preauthcodeinfo.put("message", "获取失败");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return preauthcodeinfo;
		}
		
		String accesstoken=atjo.optJSONObject("data").optString("component_access_token");
		String url=weixingetpreauthorizercode+accesstoken;
		logger.debug("get pre authorization code token");
		
		try {
			vh=RestUtils.sendRequest_buff(url, param.toString(), "POST");
			String result=(String) vh.get("message");
			logger.debug("get pre authorization code result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			String returns="获预取授权码失败！";
			if(tjo!=null&&tjo.has("pre_auth_code")) {
				preauthcodeinfo.put("code", 0);
				preauthcodeinfo.put("message", "获取成功");
				preauthcodeinfo.put("data", tjo);
			}else {
				preauthcodeinfo.put("code", -1);
				preauthcodeinfo.put("message", returns);
			}
		}catch(Exception e){
			logger.debug("get pre authorization code token error->"+e.getLocalizedMessage());
			e.printStackTrace();
			try {
				preauthcodeinfo.put("code", -1);
				preauthcodeinfo.put("message", "获取授权码失败！");
			} catch (JSONException e1) {
				
			}
		}
		
		return preauthcodeinfo;
	}
}
