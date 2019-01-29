package com.geekbrains.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ClientHandler {
    private String nickname;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public String getNickname() {
        return nickname;
    }

    public ClientHandler(Server server, Socket socket, ExecutorService service) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        service.execute(() -> {
            try {
                while (!checkAuth()) ;
                while (readMessage()) ;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ClientHandler.this.disconnect();
            }
        });
    }

//        new Thread().start();
//    }
//
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void disconnect() {
        server.unsubscribe(this);
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkAuth() throws IOException {
        String msg = in.readUTF();
        // /auth login1 pass1
        if (msg.startsWith("/auth ")) {
            String[] tokens = msg.split("\\s");
            String nick = server.getAuthService().getNicknameByLoginAndPassword(tokens[1], tokens[2]);
            if (nick != null && !server.isNickBusy(nick)) {
                sendMsg("/authok " + nick + " " + tokens[1]);
                nickname = nick;
                server.subscribe(this);
                return true;
            }
        }
        return false;
    }

    private boolean readMessage() throws IOException {
        String msg = in.readUTF();
        if (!msg.startsWith("/")) {
            server.broadcastMsg(nickname + ": " + msg);
            return true;
        }

        if (msg.equals("/end")) {
            sendMsg("/end");
            return false;
        }
        if (msg.startsWith("/w ")) {
            // /w nick2 Hello World!
            String[] tokens = msg.split("\\s", 3);
            server.privateMsg(this, tokens[1], tokens[2]);
        }
        if (msg.startsWith("/changenick ")) {
            // /changenick myNewNick
            String[] tokens = msg.split("\\s", 2);
            if (tokens[1].contains(" ")) {
                sendMsg("Ник не может содержать пробелов");
                return true;
            }
            if (server.getAuthService().changeNick(this.nickname, tokens[1])) {
                sendMsg("/yournickis " + tokens[1]);
                sendMsg("Ваш ник изменен на " + tokens[1]);
                this.nickname = tokens[1];
                server.broadcastClientsList();
            } else {
                sendMsg("Не удалось изменить ник. Такой ник " + tokens[1] + " уже существует");
            }
        }
        return true;
    }
}
