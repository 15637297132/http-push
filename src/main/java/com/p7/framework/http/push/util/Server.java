package com.p7.framework.http.push.util;

import com.alibaba.fastjson.JSON;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Yangzhen
 **/
public class Server {

    public void init(String host,int port) {
        try {
            ServerSocket serverSocket = new ServerSocket();
            System.out.println("服务器 " + host + ":" + port + " 启动成功...");
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            serverSocket.bind(socketAddress);
            AtomicInteger integer = new AtomicInteger(0);
            while (true) {
                //一旦有堵塞，表示服务器与客户端获得了连接
                Socket client = serverSocket.accept();
                System.out.println();
                int i = integer.incrementAndGet();
                System.out.println(i + ":" + client.toString());
                new HandlerThread(client, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class HandlerThread implements Runnable {
        private Socket socket;
        private int index;

        public HandlerThread(Socket socket, int index) {
            this.socket = socket;
            this.index = index;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                DataInputStream input = new DataInputStream(socket.getInputStream());
                Map<String, Object> readData = readData(input);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                String msgId = String.valueOf(readData.get("msgId"));
                String json = writeData(msgId);
                StringBuilder sb = new StringBuilder();
                sb.append("HTTP/1.1 200 ok").append("\r\n").append("Content-Type: application/json").append("\r\n").append("\r\n").append(json);
                TimeUnit.MILLISECONDS.sleep(100);
                out.write(sb.toString().getBytes());
                out.close();
                input.close();

            } catch (Exception e) {
                System.out.println(this.index + ":" + this.socket.toString() + ":" + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        socket = null;
                        System.out.println("服务端 finally 异常:" + e.getMessage());
                    }
                }
            }
        }

        public Map<String, Object> readData(DataInputStream input) throws Exception {
            byte[] bytes = new byte[input.available()];
            input.read(bytes);
            String data = new String(bytes);
            String[] split = data.split(System.getProperty("line.separator"));
            Map<String, Object> map = JSON.parseObject(split[split.length - 1], Map.class);
            System.out.println(map.get("list"));
            return map;
        }

        public String writeData(String msgId) throws Exception {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 0);
            result.put("data", msgId);
            String resultData = JSON.toJSONString(result);
            return resultData;
        }
    }

}