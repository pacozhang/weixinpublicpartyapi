package nds.publicplatform.api;

import java.util.HashMap;
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
import nds.publicweixin.ext.tools.WeixinSipStatus;

public class WeUrlLongToShort {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeUrlLongToShort.class.getName());
	private static String urllongtoshort=WebUtils.getProperty("weixin.we_url_long2short_URL","");
	
	private WxPublicControl wpc;
	private WxPublicpartyControl wppc;
	private static Hashtable<String,WeUrlLongToShort> weurllongtoshort;
	private WeUrlLongToShort(WxPublicControl wpc){
		this.wpc=wpc;
		this.wppc=wpc.getPpc();
	}
	
	public static synchronized WeUrlLongToShort getInstance(String pappid){
		if(nds.util.Validator.isNull(pappid)){return null;}
		
		WeUrlLongToShort instance=null;
		if(weurllongtoshort==null){
			weurllongtoshort=new Hashtable<String,WeUrlLongToShort>();
			WxPublicControl twpc=WxPublicControl.getInstance(pappid);
			instance=new WeUrlLongToShort(twpc);
			weurllongtoshort.put(pappid, instance);
		}else if(weurllongtoshort.containsKey(pappid)){
			instance=weurllongtoshort.get(pappid);
		}else{
			WxPublicControl twpc=WxPublicControl.getInstance(pappid);
			instance=new WeUrlLongToShort(twpc);
			weurllongtoshort.put(pappid, instance);
		}

		return instance;
	}
	
	public JSONObject long2Short(String longurl) {
		String result=null;
		String url=urllongtoshort;
		JSONObject jo=new JSONObject();
		//String token=wpc.getAccessToken();
		JSONObject atoken=wpc.getAccessToken();
		//判断ACCESSTOKEN是否获取成功
		if(atoken==null||!"0".equals(atoken.optString("code"))) {
			try {
				jo.put("code", "-1");
				jo.put("message", "请重新授权");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return jo;
		}
				
		String token=atoken.optJSONObject("data").optString("authorizer_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				jo.put("code", -1);
				jo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo;
		}
		
		HashMap<String,String> params=new HashMap<String,String>();
		params.put("access_token",token);
		try{
			url+=RestUtils.delimit(params.entrySet(), false);
		}catch(Exception e){
			result="params error:"+e.getMessage();
			try {
				jo.put("code", -1);
				jo.put("message", result);
			} catch (JSONException e1) {
				e.printStackTrace();
			}
			return jo;
		}
		
		JSONObject longurljo=new JSONObject();
		try {
			longurljo.put("action", "long2short");
			longurljo.put("long_url", longurl);
		} catch (JSONException e2) {
			logger.debug("weixin_long2short put param error->"+e2.getLocalizedMessage());
			e2.printStackTrace();
		}
		
		ValueHolder vh=null;
		logger.debug("weixin_long2short");
		try{
			vh=RestUtils.sendRequest_buff(url, longurljo.toString(), "POST");//.sendRequest(url, params, "POST");
			
			result=(String) vh.get("message");
			logger.debug("weixin_long2short result->"+result);
			
			JSONObject tjo=new JSONObject(result);
			jo= new JSONObject(result);
			String returns="地址转换失败！";
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
				e.printStackTrace();
			}
		}
		
		return jo;
	}
}
