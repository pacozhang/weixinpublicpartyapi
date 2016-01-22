package nds.publicplatform.api;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.tools.ChangeCNChar;
import nds.publicweixin.ext.tools.RestUtils;
import nds.publicweixin.ext.tools.WeixinSipStatus;

public class WeSendMessage {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeSendMessage.class.getName());
	private static String weSendMessageURL=WebUtils.getProperty("weixin.we_send_message_URL","");
	
	private static WeSendMessage instance;
	public static synchronized WeSendMessage getInstance(){
		if(instance==null){
			instance=new WeSendMessage();
		}
		
		return instance;
	}
	
	public WeSendMessage(){}
	
	public JSONObject sendMessage(WxPublicControl wc,String message){
		JSONObject jo=new JSONObject();
		String result=null;
		String url=weSendMessageURL;
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

		if(nds.util.Validator.isNull(message)){
			result="message is null!";
			try {
				jo.put("code", "-1");
				jo.put("message", "回复内容不能为空");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return jo;
		}
		message=ChangeCNChar.change(message);
		HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		try{
			url+=RestUtils.delimit(params.entrySet(), false);
		}catch(Exception e){}
		
		ValueHolder vh=null;
		logger.debug("send_message-->"+message);
		try{
			vh=RestUtils.sendRequest_buff(url, message, "POST");
			result=(String) vh.get("message");
			logger.debug("send message result->"+result);
			JSONObject tjo=null;
			tjo= new JSONObject(result);
			String returns="回复失败！";
			if(tjo!=null&&tjo.has("errcode")) {
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(tjo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(tjo.has("errmsg")){returns=tjo.optString("errmsg");}
				jo.put("code", tjo.optInt("errcode",-1));
				jo.put("message", returns);
			}else {
				jo.put("code", -1);
				jo.put("message", returns);
			}
		}catch(Exception e){
			result="公共平台网络通信障碍!";
			logger.debug("公共平台网络通信障碍!");
			try {
				jo.put("code", -1);
				jo.put("message", result);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			
			e.printStackTrace();
		}
		
		return jo;
	}
}
