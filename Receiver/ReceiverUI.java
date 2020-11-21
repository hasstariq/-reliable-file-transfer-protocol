import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;

public class ReceiverUI extends JFrame {
    private JLabel timeoutLabel;
    private JTextField timeoutTextField;
    private JLabel mdsLabel;
    private JTextField mdsTextField;
    private JLabel senderAddressLabel;
    private JTextField senderAddressTextField;
    private JLabel senderPortLabel;
    private JTextField senderPortTextField;
    private JLabel receiverPortLabel;
    private JTextField receiverPortTextField;
    private JLabel filenameLabel;
    private JTextField filenameTextField;
    private JLabel packetCounterLabel;
    private JTextField packetCounterTextField;
    private JButton receiveButton;
    private final Receiver receiver;
    private JLabel modeLabel;
    private JComboBox<ReceiverMode> modeComboBox;


    public ReceiverUI(Receiver receiver) {
        this.receiver = receiver;
        createComponents();
        layoutComponents();
        addListeners();
        setSize(400, 200);
        setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void addListeners() {
        receiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    receive();
                } catch (Exception ignored) {

                }
            }
        });
    }

    private void layoutComponents() {
        Container contentPane = getContentPane();

        //PANELS
        JPanel inputPanel = new JPanel(new GridLayout(8,2));

        //LABELS AND TEXTFIELDS
        inputPanel.add(timeoutLabel, BorderLayout.WEST);
        inputPanel.add(timeoutTextField, BorderLayout.CENTER);
        inputPanel.add(mdsLabel, BorderLayout.WEST);
        inputPanel.add(mdsTextField, BorderLayout.CENTER);
        inputPanel.add(senderAddressLabel, BorderLayout.WEST);
        inputPanel.add(senderAddressTextField, BorderLayout.CENTER);
        inputPanel.add(senderPortLabel, BorderLayout.WEST);
        inputPanel.add(senderPortTextField, BorderLayout.CENTER);
        inputPanel.add(receiverPortLabel, BorderLayout.WEST);
        inputPanel.add(receiverPortTextField, BorderLayout.CENTER);
        inputPanel.add(filenameLabel, BorderLayout.WEST);
        inputPanel.add(filenameTextField, BorderLayout.CENTER);
        inputPanel.add(packetCounterLabel, BorderLayout.WEST);
        inputPanel.add(packetCounterTextField, BorderLayout.CENTER);
        inputPanel.add(modeLabel, BorderLayout.WEST);
        modeComboBox.addItem(ReceiverMode.RELIABLE);
        modeComboBox.addItem(ReceiverMode.UNRELIABLE);
        inputPanel.add(modeComboBox, BorderLayout.CENTER);
        contentPane.add(inputPanel, BorderLayout.CENTER);



        contentPane.add(receiveButton, BorderLayout.SOUTH);

    }

    private void createComponents() {
        packetCounterTextField = new JTextField("0");
        packetCounterTextField.setEditable(false);
        receiveButton = new JButton("RECEIVE");
        mdsTextField = new JTextField("400");
        senderAddressTextField = new JTextField("127.0.0.1");
        senderPortTextField = new  JTextField("3321");
        receiverPortTextField = new JTextField("4455");
        filenameTextField = new JTextField("received");
        mdsLabel = new JLabel("MDS:");
        senderAddressLabel = new JLabel("Sender Address:");
        senderPortLabel = new JLabel("Sender Port:");
        receiverPortLabel = new JLabel("My Port:");
        filenameLabel = new JLabel("File name:");
        packetCounterLabel = new JLabel("# Received packets:");
        modeLabel = new JLabel("Mode:");
        modeComboBox= new JComboBox<>();
        timeoutLabel = new JLabel("Timeout:");
        timeoutTextField = new JTextField("20000");
    }

    private void receive() throws IOException {
        receiver.startReceiving(
                Integer.parseInt(timeoutTextField.getText()),
                Integer.parseInt(mdsTextField.getText()),
                filenameTextField.getText(),
                senderAddressTextField.getText(),
                Integer.parseInt(senderPortTextField.getText()),
                Integer.parseInt(receiverPortTextField.getText()),
                (ReceiverMode) modeComboBox.getSelectedItem());
    }

    public void updatePacketCounter(int packetCounter) {
        packetCounterTextField.setText(Integer.toString(packetCounter));
        packetCounterTextField.repaint();
        this.repaint();
    }
}
