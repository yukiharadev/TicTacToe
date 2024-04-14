import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;

public class TicTacToeClient {

    private JFrame frame = new JFrame("Tic Tac Toe");
    private JLabel messageLabel = new JLabel("");
    private ImageIcon icon;
    private ImageIcon opponentIcon;
    private JButton submit = new JButton("Submit");
    private static boolean CanStart = false;

    static JMenuBar mb;
    static JMenu m, m2;
    static JMenuItem exit, help;

    private Square[] board = new Square[9];
    private Square currentSquare;

    private static int PORT = 3001;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public TicTacToeClient(String serverAddress) throws Exception {


        // Kết nối đến máy chủ
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        //Setup giao diện người chơi
        messageLabel.setBackground(Color.lightGray);
        frame.getContentPane().add(messageLabel, "North");
        messageLabel.setText("Nhập tên của bạn");
        JPanel boardPanel = new JPanel();
        JPanel textPanel = new JPanel();

        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(3, 3, 2, 2));

        frame.getContentPane().add(boardPanel, "Center");
        frame.getContentPane().add(textPanel, "South");

        JTextField txt_inputname = new JTextField(20);
        textPanel.add(txt_inputname);
        textPanel.add(submit);

         // Xử lý sự kiện khi người chơi nhập tên và nhấn nút submit
        submit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.setTitle("WangHannn: " + txt_inputname.getText());
                messageLabel.setText("Xin chào: " + txt_inputname.getText());
                txt_inputname.setEnabled(false);
                submit.setEnabled(false);
                CanStart = true;
            }
        });

        mb = new JMenuBar();
        m = new JMenu("Control");
        m2 = new JMenu("Help");
        exit = new JMenuItem("Exit");
        help = new JMenuItem("Instruction");
        m.add(exit);
        m2.add(help);
        mb.add(m);
        mb.add(m2);

        frame.setJMenuBar(mb);

        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //pop-up help

        JFrame popUpMsg = new JFrame();
        help.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(popUpMsg, "Some information about the game:\n"
                        + "Criteria for a valid move:\n"
                        + "-The move is not occupied by any mark.\n"
                        + "-The move is made in the player's turn.\n"
                        + "-The move is made within the 3 x 3 board\n"
                        + "The game would continue and switch among the opposite player until it reaches either one of the following conditions:\n"
                        + "-Player 1 wins.\n"
                        + "-Player 2 wins.\n"
                        + "-Draw.");

            }
        });
        
        /// Tạo màn hình game
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (CanStart) {
                        currentSquare = board[j];
                        out.println("MOVE " + j);
                    }
                }
            });

            boardPanel.add(board[i]);
        }

    }


    //Nhận sự kiện trả về và xử lý sự kiện người chơi

    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                if (mark == 'X') {
                    icon = new ImageIcon("x.gif");
                } else {
                    icon = new ImageIcon("o.gif");
                }
                if (mark == 'X') {
                    opponentIcon = new ImageIcon("o.gif");
                } else {
                    opponentIcon = new ImageIcon("x.gif");
                }
            }

            while (true) {
                JFrame popUpMsg = new JFrame();
                response = in.readLine();

                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Vui lòng chờ..., bạn kia đang nghĩ");
                    currentSquare.setIcon(icon);
                    currentSquare.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(opponentIcon);
                    board[loc].repaint();
                    messageLabel.setText("Lượt cùa bạn đấy");
                } else if (response.startsWith("VICTORY")) {
                    JOptionPane.showMessageDialog(popUpMsg, "Chúc mừng bạn, bạn thắng");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    JOptionPane.showMessageDialog(popUpMsg, "Bạn thua");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("Draw.");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                } else if (response.startsWith("QUIT")) {
                    System.exit(0);
                }

            }
            out.println("QUIT");
        } finally {
            socket.close();
        }

    }

    //End game, muốn chơi lại hay không?

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame, "Retry?", "Play Again?", JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    static class Square extends JPanel {
        JLabel label = new JLabel((Icon) null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(Icon icon) {
            label.setIcon(icon);
        }
    }

    //Hàm này ko cần p nói @@

    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress;
            if (args.length == 0) {
                serverAddress = "localhost";
            } else {
                serverAddress = args[1];
            }
            TicTacToeClient client = new TicTacToeClient(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(500, 400);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();

            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}