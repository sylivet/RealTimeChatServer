import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer {
    private ServerSocket serverSocket;
    //創建線程遲來管理客戶端的連接線程，避免系統資源過度浪費
    private ExecutorService exec;
    //存放客戶端之間私聊的訊息
    private Map<String, PrintWriter> storeInfo;
    //初始化
    PrintWriter printWriter;
    BufferedReader bufferedReader;

    public  TCPServer(){
        try {
            serverSocket = new ServerSocket(5000);
            storeInfo = new HashMap<String,PrintWriter>();
            exec = Executors.newCachedThreadPool();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //將客戶端的訊息以Map形式存入集合中
    private void putIn(String key,PrintWriter value){
        synchronized (this){
            storeInfo.put(key,value);
        }
    }

    //將給定的輸出流從共享集合中刪除
    private synchronized void remove(String key){
        storeInfo.remove(key);
        System.out.println("當前在線人數為："+storeInfo.size());
    }

    //將給定的消息轉發給所有客戶端
    private synchronized void sendToAll(String message){
        for(PrintWriter out:storeInfo.values()){
            out.println(message);
        }
    }

    //將給定的消息轉發給私聊的客戶端
    private synchronized void sendToSomeone(String name,String message){
        PrintWriter printWriter=storeInfo.get(name);
        if(printWriter!=null){
            printWriter.println(message);
        }
    }

    public void start(){
        try{
            while (true){
                System.out.println("等待客戶端連接.....");
                Socket socket = serverSocket.accept();

                //獲取客戶端的ip地址
                InetAddress address = socket.getInetAddress();
                System.out.println("客戶端："+address.getHostAddress()+"連接成功！");
                printWriter = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);
                printWriter.println("【系統通知】 請輸入暱稱");

                //啟動一個線程，由線程來處理客戶端的請求，這樣可以再次監聽下一個客戶端的連接
                exec.execute(new ListenerClient(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ListenerClient implements Runnable{
        private Socket socket;
        private String name;

        public ListenerClient(Socket socket){
            this.socket=socket;
        }

        private String getName() throws IOException {
            try {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(),"UTF-8"));

                //讀取客戶端發來的暱稱
                while (true){
                    String nameString = bufferedReader.readLine();
                    if ((nameString.trim().length()==0) || storeInfo.containsKey(nameString)||nameString.length()>20){
                        printWriter.println("【系統通知】 錯誤，請重新輸入");
                    }else{
                        return nameString;
                    }
                }
            } catch (Exception e){
                throw e;
            }
        }

        @Override
        public void run() {
            try {
                //將客戶暱稱和其所說的內容存入共享集合HashMap中
                name = getName();
                putIn(name,printWriter);
                Thread.sleep(100);

                //服務端通知所有客戶端，某用戶上線
                sendToAll("【系統通知】 "+name+"已上線");
//                for(String entry:storeInfo.keySet()){
//                    sendToAll("目前在線的人： "+entry);
//                }
                String msgString = null;

                while ((msgString=bufferedReader.readLine())!=null){
                    //檢驗是否為私聊（格式：＠暱稱：內容）
                    if (msgString.startsWith("@")){
                        int index = msgString.indexOf(":");
                        if(index>=0){
                            //獲取暱稱
                            String theName = msgString.substring(1,index);
                            String info = msgString.substring(index+1);
                            info = name+"： "+info;
                            //將私聊訊息發送出去
                            sendToSomeone(theName,info);
                            continue;
                        }
                    }
                    //遍歷所有輸出流，將該客戶端發送的訊息轉發給所有客戶端
                    System.out.println(name+": "+msgString);
                    sendToAll(name+": "+msgString);
                }


            }catch (Exception e){
                //e.printStackTrace();
            }finally {
                remove(name);
                //通知所有客戶端，某某客戶已經下線
                sendToAll("【系統通知】"+name+"已經下線了。");

                if(socket!=null){
                    try {
                        socket.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        server.start();
    }
}
