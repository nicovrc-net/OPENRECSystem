package net.nicovrc;

import kotlin.Pair;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws Exception {

        ServerSocket svSock = new ServerSocket(9999);
        while (true){
            System.gc();
            Socket sock = svSock.accept();
            new Thread(() -> {
                try {
                    final InputStream in = sock.getInputStream();
                    final OutputStream out = sock.getOutputStream();

                    byte[] data = new byte[1000000];
                    int readSize = in.read(data);
                    if (readSize <= 0) {
                        data = null;
                        in.close();
                        out.close();
                        sock.close();
                        return;
                    }
                    data = Arrays.copyOf(data, readSize);
                    String s = new String(data, StandardCharsets.UTF_8);

                    Matcher matcher = Pattern.compile("(GET|HEAD) (.+) HTTP/(.+)").matcher(s);
                    if (!matcher.find()){
                        in.close();
                        out.close();
                        sock.close();

                        return;
                    }

                    final String head = matcher.group(1);
                    final String request = matcher.group(2);
                    final String ver = matcher.group(3);

                    String httpResponse = "";
                    byte[] httpResponseByte = null;

                    if (request.equals("/check.mp4")){

                        httpResponse = "HTTP/"+ver+" 404 Not Found\nContent-Type: text/plain; charset=utf-8\n\n404";
                        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        in.close();
                        out.close();
                        sock.close();

                        return;
                    }

                    String[] split = request.split("&host=");

                    System.out.println("https://"+split[1]+(split[0].startsWith("/") ? "" : "/")+split[0]);
                    OkHttpClient client = new OkHttpClient();
                    Request build = new Request.Builder()
                            .url("https://"+split[1]+(split[0].startsWith("/") ? "" : "/")+split[0])
                            .addHeader("Origin", "https://www.openrec.tv")
                            .addHeader("Referer", "https://www.openrec.tv/")
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0 nicovrc/2.0")
                            .build();

                    Response response = client.newCall(build).execute();
                    if (response.body() != null){
                        //System.out.println(response.code());
                        //System.out.println(response.body().string());

                        if (response.code() == 200){
                            httpResponse = "HTTP/"+ver+" 200 OK\nContent-Type: "+response.header("Content-Type") + "\n\n";
                            if (split[0].endsWith("m3u8")){
                                httpResponseByte = response.body().string().replaceAll("\\.m3u8", ".m3u8&host="+split[1]).replaceAll("\\.ts", ".ts&host="+split[1]).getBytes(StandardCharsets.UTF_8);
                            } else {
                                httpResponseByte = response.body().bytes();
                            }


                        } else {
                            httpResponse = "HTTP/"+ver+" 404 Not Found\nContent-Type: text/plain; charset=utf-8\n\n404";
                        }
                    }
                    response.close();

                    out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                    if (head.equals("GET") && httpResponseByte != null){
                        //System.out.println("write byte");
                        out.write(httpResponseByte);
                    }
                    out.flush();

                    in.close();
                    out.close();
                    sock.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

    }
}