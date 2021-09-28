import java.io.*;
import java.net.Socket;
import java.sql.Struct;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPClient {
    static private Socket socket;
    public TCPClient(){}

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        String serverIp;

        System.out.println("請設置服務器IP：");
        serverIp = scanner.next();
        socket =new Socket(serverIp,5000);
        TCPClient client = new TCPClient();
        client.start();
    }

    public void start(){
        try {
            Scanner scanner=new Scanner(System.in);
            setName(scanner);

            //接收服務器端發送過來的訊息的線程啟動
            ExecutorService exec = Executors.newCachedThreadPool();
            exec.execute(new ListenerServer());

            //建立輸出流，給服務端發訊息
            PrintWriter printWriter = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(),"UTF-8"),true);

            while (true){
                printWriter.println(scanner.nextLine());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (socket!=null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setName(Scanner scan) throws IOException {
        String name;

        //創建輸出流
        PrintWriter printWriter=new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(),"UTF-8"), true);
        //創建輸入流
        BufferedReader bufferedReader=new BufferedReader(
                new InputStreamReader(socket.getInputStream(),"UTF-8"));
        while (true){
            System.out.println("請創建您的暱稱： ");
            name=scan.nextLine();
            if (name.trim().equals("")){
                System.out.println("暱稱不得為空");
            }else {
                printWriter.println(name);
                String pass =bufferedReader.readLine();
                if(pass!=null&&(!pass.equals("OK"))){
                    System.out.println("暱稱已經被佔用，請重新輸入： ");
                }else {
                    System.out.println("暱稱"+name+"已經設置成功，可以開始聊天了");
                    break;
                }
            }
        }
    }

    class  ListenerServer implements Runnable{
        @Override
        public void run() {
            try {
                BufferedReader bufferedReader= null;
                bufferedReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(),"UTF-8"));
                String msgString;
                while ((msgString=bufferedReader.readLine())!=null){
                    System.out.println(msgString);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
