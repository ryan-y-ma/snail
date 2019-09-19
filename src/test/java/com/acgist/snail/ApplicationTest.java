package com.acgist.snail;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;

import com.acgist.snail.net.application.ApplicationClient;
import com.acgist.snail.net.application.ApplicationServer;
import com.acgist.snail.pojo.message.ApplicationMessage;
import com.acgist.snail.pojo.message.ApplicationMessage.Type;
import com.acgist.snail.system.bencode.BEncodeEncoder;
import com.acgist.snail.utils.ThreadUtils;

public class ApplicationTest {

	@Test
	public void server() {
		ApplicationServer.getInstance().listen();
		ThreadUtils.sleep(Long.MAX_VALUE);
	}
	
	@Test
	public void client() {
		final ApplicationClient client = ApplicationClient.newInstance();
		client.connect();
		String message = null;
		Scanner scanner = new Scanner(System.in);
//		while ((message = scanner.next()) != null) { // 使用next()读取时会按照空白行（空格、Tab、Enter）拆分，使用nextLine()不会被拆分。
		while ((message = scanner.nextLine()) != null) {
			if(message.equals("close")) {
				client.send(ApplicationMessage.message(Type.close, message));
				client.close();
				break;
			} else if(message.equals("shutdown")) {
				client.send(ApplicationMessage.message(Type.shutdown, message));
				client.close();
				break;
			} else if(message.equals("gui")) {
				client.send(ApplicationMessage.message(Type.gui, message));
			} else if(message.equals("notify")) {
				client.send(ApplicationMessage.message(Type.notify, message));
			} else if(message.equals("taskNew")) {
				Map<String, String> map = new HashMap<>();
				map.put("url", "http://mirror.bit.edu.cn/apache/tomcat/tomcat-9/v9.0.24/bin/apache-tomcat-9.0.24-windows-x86.zip");
//				map.put("url", "E:\\snail\\0000.torrent");
//				map.put("files", "l50:[UHA-WINGS][Vinland Saga][01][x264 1080p][CHT].mp4e");
				client.send(ApplicationMessage.message(Type.taskNew, BEncodeEncoder.encodeMapString(map)));
			} else if(message.equals("taskList")) {
				client.send(ApplicationMessage.message(Type.taskList, message));
			} else if(message.equals("taskStart")) {
				client.send(ApplicationMessage.message(Type.taskStart, "37f48162-d306-4fff-b161-f1231a3f7e48"));
			} else if(message.equals("taskPause")) {
				client.send(ApplicationMessage.message(Type.taskPause, "37f48162-d306-4fff-b161-f1231a3f7e48"));
			} else if(message.equals("taskDelete")) {
				client.send(ApplicationMessage.message(Type.taskDelete, "37f48162-d306-4fff-b161-f1231a3f7e48"));
			} else {
				client.send(ApplicationMessage.text(message));
			}
		}
		scanner.close();
	}
	
}