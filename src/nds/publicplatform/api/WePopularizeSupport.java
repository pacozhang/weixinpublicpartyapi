package nds.publicplatform.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import com.swetake.util.Qrcode;

import nds.control.util.ValueHolder;
import nds.control.web.WebUtils;
import nds.log.Logger;
import nds.log.LoggerManager;
import nds.publicweixin.ext.common.WxPublicControl;
import nds.publicweixin.ext.tools.RestUtils;
import nds.publicweixin.ext.tools.WeixinSipStatus;
import nds.util.ImageUtils;

public class WePopularizeSupport {
	private static Logger logger= LoggerManager.getInstance().getLogger(WePopularizeSupport.class.getName());
	private static String createquickmarkurl=WebUtils.getProperty("weixin.we_create_quickmark_URL","");
	private static String getquickmarkurl=WebUtils.getProperty("weixin.we_get_quickmark_URL","");
	
	private Quickmark qk=null; 

	/**
	 * 创建临时二维码
	 * @param wc
	 * @param pjo
	 * @return
	 */
	public JSONObject createTemporaryQuickmark(WxPublicControl wc,JSONObject pjo){
		String url=createquickmarkurl;
		String result=null;
		String ticket=null;
		JSONObject jo=new JSONObject();
		int validTime=pjo.optInt("validTime",604800);
		String scene_id=pjo.optString("scene_id");

		//String token=wc.getAccessToken();
		JSONObject atoken=wc.getAccessToken();
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
				jo.put("code", "-1");
				jo.put("message", "请查看APPID与APPSECRET是否正确！");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo;
		}
		
		HashMap<String,String> paras=new HashMap<String,String>();
		paras.put("access_token",token);
		
		String message="{\"expire_seconds\":"+validTime;
		//message+=",\"action_name\":\"QR_SCENE\",\"action_info\": {\"scene\": {\"scene_id\":"+scene_id+"}}}";
		
		message+=",{\"action_name\":\"QR_SCENE\",\"action_info\":"+pjo.optString("action_info")+"}}";

		ValueHolder vh=null;
		logger.debug("create_temporary_quickmark");
		try{
			url+=RestUtils.delimit(paras.entrySet(), true);

			vh=RestUtils.sendRequest_buff(url, message, "POST");
			result=(String) vh.get("message");
			logger.debug("create_temporary_quickmark result->"+result);
			JSONObject tjo= new JSONObject(result);
			String returns="创建临时二唯码失败！";
			if(tjo!=null&&tjo.has("errcode")) {
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(jo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(jo.has("errmsg")){returns=tjo.optString("errmsg");}
				jo.put("code", tjo.optInt("errcode",-1));
				jo.put("message", returns);
			}else if(tjo!=null&&tjo.has("ticket")) {
				jo.put("code", 0);
				jo.put("message", "创建临时二唯码成功");
				jo.put("ticket", tjo.optString("ticket"));
				jo.put("expire_seconds", tjo.optInt("expire_seconds",1800));
				jo.put("url", tjo.optString("url"));
			}else {
				jo.put("code", -1);
				jo.put("message", returns);
			}
		}catch(Exception e){
			result="公共平台网络通信障碍!";
			logger.debug("公共平台网络通信障碍!"+e.getMessage());
		}
		
		return jo;
	}
	
	/**
	 * 创建永久二维码
	 * @param wc
	 * @param pjo
	 * @return
	 */
	public JSONObject createPermanenceQuickmark(WxPublicControl wc,JSONObject pjo){
		String url=createquickmarkurl;
		String result=null;
		String ticket=null;
		JSONObject jo=new JSONObject();
		
		//String token=wc.getAccessToken();
		JSONObject atoken=wc.getAccessToken();
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
				jo.put("message", "请查看APPID与APPSECRET是否正确！");
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jo;
		}
		
		String scene_id=pjo.optString("scene_id");
		HashMap<String,String> paras=new HashMap<String,String>();
		paras.put("access_token",token);

		//String message ="{\"action_name\":\"QR_LIMIT_STR_SCENE\",\"action_info\": {\"scene\": {\"scene_id\":\"store"+scene_id+"\"}}}";
		String message ="{\"action_name\":\"QR_LIMIT_STR_SCENE\",\"action_info\":"+pjo.optString("action_info")+"}}";
		ValueHolder vh=null;
		logger.debug("create_permanence_quickmark");
		try{
			url+=RestUtils.delimit(paras.entrySet(), true);

			vh=RestUtils.sendRequest_buff(url, message, "POST");
			result=(String) vh.get("message");
			logger.debug("create_permanence_quickmark result->"+result);
			JSONObject tjo= new JSONObject(result);
			String returns="创建永久二唯码失败！";
			if(tjo!=null&&tjo.has("errcode")) {
				WeixinSipStatus s=WeixinSipStatus.getStatus(String.valueOf(jo.optInt("errcode",-1)));
				if(s!=null) {returns=s.toString();}
				else if(jo.has("errmsg")){returns=tjo.optString("errmsg");}
				jo.put("code", tjo.optInt("errcode",-1));
				jo.put("message", returns);
			}else if(tjo!=null&&tjo.has("ticket")) {
				jo.put("code", 0);
				jo.put("message", "创建永久二唯码成功");
				jo.put("ticket", tjo.optString("ticket"));
				jo.put("expire_seconds", tjo.optInt("expire_seconds",1800));
				jo.put("url", tjo.optString("url"));
			}else {
				jo.put("code", -1);
				jo.put("message", returns);
			}
		}catch(Exception e){
			logger.debug("公共平台网络通信障碍->"+e.getMessage());
			try {
				jo.put("code", -1);
				jo.put("message", "公共平台网络通信障碍->"+e.getMessage());
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		
		return jo;
	}
	
	/**
	 * 从微信下载二维码
	 * @param ticket
	 * @param filePath
	 * @return
	 */
	public JSONObject getQuickmark(String ticket,String filePath){
		String url=getquickmarkurl;
		byte[] result=null;
		JSONObject jo=null;
		HashMap<String,String> paras=new HashMap<String,String>();

		ValueHolder vh=null;
		logger.debug("get_quickmark");
		try{
			paras.put("ticket",URLEncoder.encode(ticket,"utf-8"));
			url+=RestUtils.delimit(paras.entrySet(), false);
			
			vh=RestUtils.sendRequest_buffs(url, null, "GET");
			result=(byte[])vh.get("message");
			OutputStream os = new FileOutputStream(filePath);
			os.write(result);
			os.close();
			logger.debug("resulttype->"+vh.get("message").getClass().getSimpleName());
		}catch(Exception e){
			logger.debug("公共平台网络通信障碍!");
		}
		
		return jo;
	}
	
	/**
	 * 生成二维码(QRCode)图片
	 * @param content 存储内容
	 * @param imgPath 图片路径
	 * @param imgType 图片类型
	 * @param size 二维码尺寸
	 * @param logoPath logo图片地址
	 */
	public JSONObject encoderQRCode(String content, String imgPath, String imgType, int size, String logoPath) {
		JSONObject jo=new JSONObject();
		try {
			BufferedImage bufImg = this.crateRCodeCommon(content, imgType, size, logoPath);
			
			File f=new File(imgPath);
			if(!f.exists()) {f.mkdirs();}
			// 生成二维码QRCode图片
			ImageIO.write(bufImg, imgType, f);
			jo.put("code", 0);
			jo.put("message", "操作成功");
		} catch (Exception e) {
			e.printStackTrace();
			try {
				jo.put("code", -1);
				jo.put("message", e.getMessage());
			}catch(Exception e1) {
				
			}
		}
		return jo;
	}
	
	/** 
	 * 生成二维码(QRCode)图片的公共方法
	 * @param content 存储内容
	 * @param imgType 图片类型
	 * @param size 二维码尺寸
	 * @param logoPath logo图片地址
	 * @return
	 * @throws Exception 
	 */
	private BufferedImage crateRCodeCommon(String content, String imgType, int size, String logoPath) throws Exception {
		BufferedImage bufImg = null;
		try {
			Qrcode qrcodeHandler = new Qrcode();
			// 设置二维码排错率，可选L(7%)、M(15%)、Q(25%)、H(30%)，排错率越高可存储的信息越少，但对二维码清晰度的要求越小
			qrcodeHandler.setQrcodeErrorCorrect('L');
			qrcodeHandler.setQrcodeEncodeMode('B');
			// 设置设置二维码尺寸，取值范围1-40，值越大尺寸越大，可存储的信息越大
			qrcodeHandler.setQrcodeVersion(9);
			// 获得内容的字节数组，设置编码格式
			byte[] contentBytes = content.getBytes("utf-8");
			// 图片尺寸
			int imgSize = 67 + 12 * (size - 1);
			bufImg = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
			Graphics2D gs = bufImg.createGraphics();
			// 设置背景颜色
			gs.setBackground(null);
			gs.clearRect(0, 0, imgSize, imgSize);

			// 设定图像颜色> BLACK
			gs.setColor(new Color(0, 84, 165));
			// 设置偏移量，不设置可能导致解析出错
			int pixoff = 2;
			// 输出内容> 二维码
			if (contentBytes.length > 0 && contentBytes.length < 800) {
				boolean[][] codeOut = qrcodeHandler.calQrcode(contentBytes);
				for (int i = 0; i < codeOut.length; i++) {
					for (int j = 0; j < codeOut.length; j++) {
						if (codeOut[j][i]) {
							gs.fillRect(j * 5 + pixoff, i * 5 + pixoff, 5, 5);
						}
					}
				}
			} else {
				throw new Exception("QRCode content bytes length = " + contentBytes.length + " not in [0, 800].");
			}
			
			if(nds.util.Validator.isNotNull(logoPath)){
				//读取头像图片，调整图片大小。
				//byte[] bos =changePicSize(logo);
				//ByteArrayInputStream bais = new ByteArrayInputStream(bos);
				//BufferedImage bi1 =ImageIO.read(bais);
				//URL url = new URL(logo);
				//System.out.print(logo);
				logger.debug("logoPath is："+logoPath);
				try{
					BufferedImage bi1=ImageUtils.getScaledInstance(logoPath,60,60,"png",true); 
					//Image img = ImageIO.read(new File(ccbPath));//实例化一个Image对象。
					//读取图片，设置图片在二维码中起始位置。
					//System.out.print(imgSize);
					gs.drawImage(bi1, 106,106, null);
				}catch(Exception w){
					logger.debug("logopath file is not find->"+logoPath);
				}
			}
			gs.dispose();
			bufImg.flush();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("创建二维码失败："+e.getMessage());
		}
		return bufImg;
	}
	
	private class Quickmark{
		private int validTime;
		private String quickmarkType;
		private int scene_id;
		private String ticket;
	}
}
