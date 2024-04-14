import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class TicTacToeServer {

    // Phương thức main của server
    public static void main(String[] args) throws Exception {
        ServerSocket listener = new ServerSocket(3001); // Tạo một ServerSocket để lắng nghe các kết nối từ các client
        System.out.println("Server is Running"); // Hiển thị thông điệp thông báo rằng server đã chạy
        try {
            while (true) {
                Game game = new Game(); // Tạo một trò chơi mới
                Game.Player playerX = game.new Player(listener.accept(), 'X'); // Chấp nhận kết nối từ người chơi X
                Game.Player playerO = game.new Player(listener.accept(), 'O'); // Chấp nhận kết nối từ người chơi O
                playerX.setOpponent(playerO); // Thiết lập đối thủ của người chơi X là người chơi O
                playerO.setOpponent(playerX); // Thiết lập đối thủ của người chơi O là người chơi X
                game.currentPlayer = playerX; // Thiết lập người chơi hiện tại là người chơi X
                playerX.start(); // Bắt đầu luồng của người chơi X
                playerO.start(); // Bắt đầu luồng của người chơi O
            }
        } finally {
            listener.close(); // Đóng ServerSocket sau khi kết thúc trò chơi
        }
    }
}

class Game {
    private Player[] board = {
            null, null, null,
            null, null, null,
            null, null, null };

    Player currentPlayer;

    // Kiểm tra xem có người chiến thắng hay không
    public boolean hasWinner() {
        return (board[0] != null && board[0] == board[1] && board[0] == board[2])
                || (board[3] != null && board[3] == board[4] && board[3] == board[5])
                || (board[6] != null && board[6] == board[7] && board[6] == board[8])
                || (board[0] != null && board[0] == board[3] && board[0] == board[6])
                || (board[1] != null && board[1] == board[4] && board[1] == board[7])
                || (board[2] != null && board[2] == board[5] && board[2] == board[8])
                || (board[0] != null && board[0] == board[4] && board[0] == board[8])
                || (board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    // Kiểm tra xem còn ô để đi không
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }

    // Kiểm tra xem nước đi có hợp lệ không
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }


    class Player extends Thread {
        char mark;
        Player opponent;
        Socket socket;
        BufferedReader input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("WELCOME " + mark); 
                output.println("MESSAGE Enter your player name"); // Yêu cầu người chơi nhập tên
            } catch (IOException e) {
                System.out.println("Player disconnected: " + e); // Hiển thị thông điệp lỗi nếu có lỗi xảy ra
            }
        }

        // Thiết lập đối thủ của người chơi
        public void setOpponent(Player opponent) {
            this.opponent = opponent;
        }

        // Phương thức này gửi thông điệp cho client
        public void otherPlayerMoved(int location) {
            output.println("OPPONENT_MOVED " + location);
            if (hasWinner()) {
                output.println("DEFEAT");
            } else if (boardFilledUp()) {
                output.println("TIE"); 
            } else {
                output.println(""); 
            }
        }


        public void run() {
            try {
                if (mark == 'X') {
                    output.println("MESSAGE Your move"); 
                }

                while (true) {
                    String command = input.readLine();
                    if (command.startsWith("MOVE")) {
                        int location = Integer.parseInt(command.substring(5));
                        if (legalMove(location, this)) { 
                            output.println("VALID_MOVE");
                            if (hasWinner()) {
                                output.println("VICTORY");
                            } else if (boardFilledUp()) {
                                output.println("TIE"); 
                            } else {
                                output.println(""); 
                            }
                        } else {
                            output.println("MESSAGE Invalid move"); 
                        }

                    } else if (command.startsWith("QUIT")) { 
                        return; 
                    }
                }
            } catch (IOException e) {
                System.out.println("Player died: " + e); // Hiển thị thông điệp lỗi nếu có 1 người out
                JFrame popUpMsg = new JFrame();
                JOptionPane.showMessageDialog(popUpMsg, "Bạn thắng, người chơi kia đã out game"); 
                output.println("CLOSE"); 
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
