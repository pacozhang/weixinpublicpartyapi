package nds.publicplatform.api;

import java.util.HashMap;
import java.util.Hashtable;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.tools.RestUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class WeTemplate {

	private static Logger logger= LoggerManager.getInstance().getLogger(WeTemplate.class.getName());
	private static String we_sendTemplate_URL=WebUtils.getProperty("weixin.we_sendTemplate_URL","https://api.weixin.qq.com/cgi-bin/message/template/send?");
	
	
	private static Hashtable<String,WeTemplate> weTemplates;
	private WeTemplate(){
	}
	
	public static synchronized WeTemplate getInstance(String pappid){
		if(nds.util.Validator.isNull(pappid)){return null;}
		
		WeTemplate instance=null;
		if(weTemplates==null){
			weTemplates=new Hashtable<String,WeTemplate>();
			instance=new WeTemplate();
			weTemplates.put(pappid, instance);
		}else if(weTemplates.containsKey(pappid)){
			instance=weTemplates.get(pappid);
		}else{
			instance=new WeTemplate();
			weTemplates.put(pappid, instance);
		}

		return instance;
	}	
	
	
	public JSONObject sendTemplate(WxPublicControl wc,String senddata){
		logger.debug("sendTemplate");
		String returnr=null;
		String url=we_sendTemplate_URL;
		JSONObject resultjo=new JSONObject();
		JSONObject atoken=wc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				resultjo.put("code", "-1");
				resultjo.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return resultjo;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				resultjo.put("code", -1);
				resultjo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return resultjo;
		}
		
		
		ValueHolder vh=null;
        	
    	HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
 
    	logger.debug("sendTemplate  begin...");
    	try{
    		url+=RestUtils.delimit(params.entrySet(), true);
        	vh=RestUtils.sendRequest_buff(url, senddata, "POST");
        	String result=(String) vh.get("message");
        	logger.debug("sendTemplate result->"+result);
        	JSONObject jo=new JSONObject(result);
        	if(jo!=null&&jo.has("errcode")){
        		resultjo.put("code", jo.optString("errcode"));
    			resultjo.put("message",jo.optString("errmsg"));
    			resultjo.put("msgid", jo.optString("msgid"));
        	}else{
        		returnr=result;
        		resultjo.put("code", -1);
    			resultjo.put("message", returnr);
        	}
        	
    	}catch (Throwable tx) {
    		returnr="sendTemplate error:"+tx.getMessage();
    		try {
				resultjo.putOpt("code", -1);
				resultjo.putOpt("message", returnr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	 		logger.debug("sendTemplate error: "+tx.getMessage());
	 	}
    
        return resultjo;
	}



}
