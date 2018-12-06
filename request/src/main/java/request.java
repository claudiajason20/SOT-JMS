import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

public class request {
    public static void main(String args[]){
        Connection connection; // to connect to the ActiveMQ
        Session session; // session for creating messages, producers and
        Destination sendDestination; // reference to a queue/topic destination
        MessageProducer producer; // for sending messages
        Destination receiveDestination; //reference to a queue/topic destination
        MessageConsumer consumer; // for receiving messages
        Gson gson = new GsonBuilder().create();

        ArrayList<Message> messages= new ArrayList<>();

        Random rand = new Random();
        int eqw = rand.nextInt(50);

        String receive = "myReplyDestination" + Integer.toString(eqw);

        JFrame frame = new JFrame("JMSRequest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel jp = new JPanel();
        JPanel jp2 = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        jp.setLayout(gridbag);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JLabel a = new JLabel("name");
        JTextField name = new JTextField(32);
        JLabel b = new JLabel("address");
        JTextField address = new JTextField(32);
        JLabel d = new JLabel("class");
        JTextField classes = new JTextField(32);
        d.setLabelFor(classes);

        jp.add(a);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(name, c);
        jp.add(name);

        //jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));

        jp.add(b);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(address, c);
        jp.add(address);

        jp.add(d);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(classes, c);
        jp.add(classes);

        JButton btnAdd = new JButton();
        btnAdd.setText("Request");
        jp.add(btnAdd);

        JTable table = new JTable();
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Message Sent");
        model.addColumn("Message Sent ID");
        model.addColumn("Message Replied");
        table.setModel(model);

        table.removeColumn(table.getColumnModel().getColumn(1));

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
            // connect to the Destination called “myFirstChannel”
            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
            props.put(("queue."+receive), receive);
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
// connect to the receiver destination
            receiveDestination = (Destination) jndiContext.lookup(receive);
            consumer = session.createConsumer(receiveDestination);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message msg) {
                    if (msg instanceof TextMessage) {
                        try {
                            System.out.println("received: " + msg);
                            String received = ((TextMessage) msg).getText();
                            String corr = msg.getJMSCorrelationID();
                            int index = getRowByValue(model,corr);
                            String send = table.getValueAt(index,0).toString();
                            model.setValueAt(received,index,2);
                            table.setModel(model);
                        }
                        catch (Exception ex) {
                            // throw new RuntimeException(ex);
                            ex.printStackTrace();
                        }
                    }
                    else {
                        // throw new IllegalArgumentException("Message must be of type TextMessage");
                    }
                }
            });
            connection.start(); // this is needed to start receiving messages
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
            // connect to the Destination called “myFirstChannel”
            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
            props.put(("queue.myRequestDestination"), "myRequestDestination");
            props.put(("queue."+receive),receive);
            Context jndiContext = new InitialContext(props);
            Destination finalDestination = (Destination) jndiContext.lookup(receive);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            // connect to the sender destination
            sendDestination = (Destination) jndiContext.lookup("myRequestDestination");
            producer = session.createProducer(sendDestination);

            Session finalSession = session;


            btnAdd.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //String body = name.getText(); //or serialize an object!

                    Student student = new Student(name.getText(),classes.getText(),address.getText());
                    String json = gson.toJson(student);

                    String id = null;

                    // create a text message
                    Message msg = null;
                    try {
                        msg = finalSession.createTextMessage(json);
                        msg.setJMSReplyTo(finalDestination);
                        // send the message
                        producer.send(msg);
                        messages.add(msg);
                        System.out.println("JMSMessageID=" + msg.getJMSMessageID()
                                + " JMSDestination=" + msg.getJMSDestination()
                                + " Text=" + ((TextMessage) msg).getText());
                        id = msg.getJMSMessageID();
                        model.addRow(new String[]{name.getText(),id});
                        table.setModel(model);
                        name.setText("");
                        classes.setText("");
                        address.setText("");
                        // print all message attributes; but JMSDestination is null
                        // session makes the message via ActiveMQ. AtiveMQ assigns unique JMSMessageID
                        // to each message.
                        //print all message attributes; but JMSDestination is senderDestination name
                        System.out.println(msg);
                    } catch (JMSException e1) {
                        e1.printStackTrace();
                    }



                }
            });

        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }



        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(450,250));
        jp2.add(scrollPane, BorderLayout.CENTER);

        jp.setPreferredSize(new Dimension(450,150));
        jp2.setSize(450,300);
        frame.add(jp,BorderLayout.CENTER);
        frame.add(jp2,BorderLayout.AFTER_LAST_LINE);
        frame.setSize(500, 400);
        frame.setVisible(true);

    }
    static int getRowByValue(TableModel model, Object value) {
        for (int i = model.getRowCount() - 1; i >= 0; --i) {
            for (int j = model.getColumnCount() - 1; j >= 0; --j) {
                if (model.getValueAt(i, j) != null && model.getValueAt(i, j).equals(value)){
                    // what if value is not unique?
                    return i;
                }
            }
        }
        return 0;
    }
}
