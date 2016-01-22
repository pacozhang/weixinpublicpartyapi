package nds.publicplatform.api;

import java.util.HashMap;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.common.WxPublicpartyControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.util.NDSException;
import nds.publicweixin.ext.tools.WeixinSipStatus;

public class WeMeun {
	private static Logger logger= LoggerManager.getInstance().getLogger(WeMeun.class.getName());
	private static String weCreateURL=WebUtils.getProperty("weixin.we_create_meun_URL","");
	private static String weGetURL=WebUtils.getProperty("weixin.we_get_meun_URL","");
	private static String weDeleteURL=WebUtils.getProperty("weixin.we_delete_meun_URL","");
	
	private WxPublicControl wpc;
	private WxPublicpartyControl wppc;
	private static Hashtable<String,WeMeun> wmenus;
	private WeMeun(WxPublicControl wpc){
		this.wpc=wpc;
		this.wppc=wpc.getPpc();
	}
	
	/**
	 * 
	 * @param pappid 公众号APPID
	 * @return
	 */
	public static synchronized WeMeun getInstance(String pappid){
		if(nds.util.Validator.isNull(pappid)){return null;}
		
		WeMeun instance=null;
		if(wmenus==null){
			wmenus=new Hashtable<String,WeMeun>();
			
			WxPublicControl twpc=WxPublicControl.getInstance(pappid);
			instance=new WeMeun(twpc);
			wmenus.put(pappid, instance);
		}else if(wmenus.containsKey(pappid)){
			instance=wmenus.get(pappid);
		}else{
			WxPublicControl twpc=WxPublicControl.getInstance(pappid);
			instance=new WeMeun(twpc);
			wmenus.put(pappid, instance);
		}

		return instance;
	}
	
	public JSONObject createMenu(String menus) throws NDSException{
		String result=null;
		JSONObject jo=new JSONObject();
		JSONObject sumo=null;
		JSONObject jomenus=null;
		String url=weCreateURL;
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
		
		
		/*String purl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+wc.getAppid()+"&redirect_uri=http://www.demo.com/index.jsp&response_type=code&scope=snsapi_base&state=1#wechat_redirect"; 
		if(nds.util.Validator.isNull(menus)) {
			menus="{button:[{\"type\":\"click\",name:\"今日歌曲\",\"key\":\"V1001_TODAY_MUSIC\"},";
			menus+="{\"type\":\"click\",\"name\":\"歌手简介\",\"key\":\"V1001_TODAY_SINGER\"},";
			menus+="{\"name\":\"菜单\",\"sub_button\":[";
			menus+="{\"type\":\"view\",\"name\":\"搜索\",\"url\":\"http://www.google.com/\" },";
			menus+="{\"type\":\"view\",\"name\":\"视频\",\"url\":\""+purl+"\"},";
			menus+="{\"type\":\"click\",\"name\":\"赞一下我们\",\"key\":\"V1001_GOOD\"}";
			menus+="]}]}";
		}
		
		if(nds.util.Validator.isNull(menus)) {
			String gwurl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+wc.getAppid()+"&redirect_uri=http://www.demo.com&response_type=code&scope=snsapi_base&state=1#wechat_redirect";
			String scurl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+wc.getAppid()+"&redirect_uri=http://www.demo.com/mall.jsp&response_type=code&scope=snsapi_base&state=1#wechat_redirect";
			String hyurl="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+wc.getAppid()+"&redirect_uri=http://www.demo.com/html/nds/oto/webapp/usercenter/index.vml&response_type=code&scope=snsapi_base&state=1#wechat_redirect";
			menus="{button:[{\"type\":\"view\",\"name\":\"伯俊商城\",\"url\":\""+scurl+"\"}";
			menus+=",{\"type\":\"view\",\"name\":\"伯俊官网\",\"url\":\""+gwurl+"\"}";
			menus+=",{\"type\":\"view\",\"name\":\"伯俊会员\",\"url\":\""+hyurl+"\"}]}";
		}
		try{jomenus=new JSONObject(menus);}
		catch(Exception e){}
		
		JSONArray subMenus=null;
		
		if(menus==null){
			returns="button is null!";
			logger.debug("button is null!");
			throw new NDSException("button is null");
		}
		JSONArray buttons=jomenus.optJSONArray("button");
		
		if(buttons!=null&&buttons.length()>3){
			returns="button length larger than 3!";
			logger.debug("button length larger than 3!");
			throw new NDSException("button length larger than 3");
		}
		
		for(int i=0;i<buttons.length();i++){
			sumo=(JSONObject)buttons.optJSONObject(i);
			if(sumo!=null&&sumo.has("sub_button")){
				subMenus=sumo.optJSONArray("sub_button");
				if(subMenus.length()>5){
					returns="sub_button length larger than 5!";
					logger.debug("sub_button length larger than 5!");
					throw new NDSException("sub_button length larger than 5");
				}
			}
		}*/
		
		HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		try{
			url+=RestUtils.delimit(params.entrySet(), false);
		}catch(Exception e){}
		
		ValueHolder vh=null;
		logger.debug("create_menu");
		try{
			vh=RestUtils.sendRequest_buff(url,menus,"POST");
			result=(String) vh.get("message");
			logger.debug("create menu result->"+result);
			JSONObject tjo=new JSONObject(result);
			String returns="创建菜单失败！";
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
		} catch (Throwable tx) {
			try {
				jo.put("code", "-1");
				jo.put("message", "公共平台网络通信障碍->"+tx.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			logger.debug("公共平台网络通信障碍->"+tx.getMessage());
		}
		return jo;
	}
		
	public JSONObject getMenus(){
		String result=null;
		JSONObject jo=null;
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
			jo=new JSONObject();
			try {
				jo.put("code", -1);
				jo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo;
		}
		
		HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		
		ValueHolder vh=null;
		logger.debug("get_menu");
		try{
			vh=RestUtils.sendRequest(weGetURL,params,"POST");
			result=(String) vh.get("message");
			JSONObject tjo=new JSONObject(result);
			jo= new JSONObject();
			String returns="获取菜单失败！";
			if(tjo!=null&&tjo.has("errcode")) {
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(tjo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(tjo.has("errmsg")){returns=tjo.optString("errmsg");}
				jo.put("code", jo.optInt("errcode",-1));
				jo.put("message", returns);
			}else if(tjo!=null&&tjo.has("button")) {
				jo.put("code", 0);
				jo.put("message", result);
			}else {
				jo.put("code", -1);
				jo.put("message", returns);
			}
		} catch (Throwable tx) {
			logger.debug("公共平台网络通信障碍!");
			try {
				jo.put("code", -1);
				jo.put("message", "公共平台网络通信障碍");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			//throw new NDSException("sub_button length larger than 5",tx);
		}
		return jo;
	}
	
	public JSONObject deleteMenus(){
		String result=null;
		JSONObject jo=new JSONObject();
		JSONObject atjo=wppc.getAccessToken();
		
		//判断ACCESSTOKEN是否获取成功
		if(atjo==null||!"0".equals(atjo.optString("code"))) {
			return atjo;
		}
		String token=atjo.optJSONObject("data").optString("component_access_token");
		if(nds.util.Validator.isNull(token)) {
			try {
				jo.put("code", -1);
				jo.put("message", "请重新授权");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo;
		}
		
		HashMap<String, String> params =new HashMap<String, String>();
		params.put("access_token",token);
		
		ValueHolder vh=null;
		logger.debug("delete_menu");
		try{
			vh=RestUtils.sendRequest(weDeleteURL,params,"POST");
			result=(String) vh.get("message");
			logger.debug("delete menu result->"+result);
			JSONObject tjo=new JSONObject(result);
			String returns="删除菜单失败！";
			if(tjo!=null&&tjo.has("errcode")) {
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(tjo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(tjo.has("errmsg")){returns=tjo.optString("errmsg");}
				jo= new JSONObject();
				jo.put("code", tjo.optInt("errcode",-1));
				jo.put("message", tjo.optString("errmsg"));
			}else {
				jo= new JSONObject();
				jo.put("code", -1);
				jo.put("message", returns);
			}
		} catch (Throwable tx) {
			logger.debug("公共平台网络通信障碍->"+tx.getMessage());
			try {
				jo=new JSONObject();
				jo.put("code", "-1");
				jo.put("message", "公共平台网络通信障碍->"+tx.getMessage());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			//throw new NDSException("sub_button length larger than 5",tx);
		}
		
		return jo;
	}
}
