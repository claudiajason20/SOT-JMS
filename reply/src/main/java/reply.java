import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;

public class reply {
    static Destination finalDestination;
    static Connection connection; // to connect to the JMS
    static Session session; // session for creating consumers
    static Destination receiveDestination; //reference to a queue/topic destination
    static MessageConsumer consumer; // for receiving messages
    static Destination sendDestination; // reference to a queue/topic destination
    static MessageProducer producer; // for sending messages

    public static void main(String args[]){
        Gson gson = new GsonBuilder().create();
        ArrayList<Message> messages = new ArrayList<>();

        JFrame frame = new JFrame("JMSReply");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel jp = new JPanel();

        JTextField input = new JTextField(30);
        jp.add(input, BorderLayout.CENTER);

        JButton btnAdd = new JButton();
        btnAdd.setText("Reply");
        jp.add(btnAdd,BorderLayout.AFTER_LAST_LINE);

        JTable table = new JTable();
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Name");
        model.addColumn("Class");
        model.addColumn("Address");
        model.addColumn("Data Noted?");
        table.setModel(model);

        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
            // connect to the Destination called “myFirstChannel”
            // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
            props.put(("queue.myRequestDestination"), " myRequestDestination");
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
// connect to the receiver destination
            receiveDestination = (Destination) jndiContext.lookup("myRequestDestination");
            consumer = session.createConsumer(receiveDestination);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message msg) {
                    if (msg instanceof TextMessage) {
                        try {
                            System.out.println("received: " + msg);
                            messages.add(msg);
                            String receive = ((TextMessage) msg).getText();
                            Student s = gson.fromJson(receive,Student.class);
                            String corr = ((TextMessage) msg).getJMSMessageID();
                            model.addRow(new String[]{s.getName(),s.getClasses(),s.getAddress()});
                            table.setModel(model);
                        }
                        catch (JMSException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    else {
                        throw new IllegalArgumentException("Message must be of type TextMessage");
                    }
                }
            });
            connection.start(); // this is needed to start receiving messages
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }




        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Properties props = new Properties();
                props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                        "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
                props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61616");
                // connect to the Destination called “myFirstChannel”
                // queue or topic: “queue.myFirstDestination” or “topic.myFirstDestination”
                int row = table.getSelectedRow();
                String text = table.getValueAt(row,0).toString();
                System.out.println(text);
                for (int i = 0;i < messages.size();i++){

                    try {
                        String c = ((TextMessage) messages.get(i)).getText();
                        Student j = gson.fromJson(c,Student.class);
                        if (j.getName().equals(text)){
                            finalDestination =  messages.get(i).getJMSReplyTo();
                        }
                    } catch (JMSException e1) {
                        e1.printStackTrace();
                    }
                }
                props.put((String.valueOf(finalDestination)), String.valueOf(finalDestination));
                System.out.println(finalDestination);
                Context jndiContext = null;
                try {
                    jndiContext = new InitialContext(props);
                    ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
                    connection = connectionFactory.createConnection();
                    session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                    // connect to the sender destination
                    //sendDestination = (Destination) jndiContext.lookup(finalDestination);
                    producer = session.createProducer(finalDestination);
                } catch (NamingException e1) {
                    e1.printStackTrace();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }


                Session finalSession = session;

                String body = input.getText(); //or serialize an object!
                model.setValueAt(body,table.getSelectedRow(),3);
                table.setModel(model);
                input.setText("");

                // create a text message
                Message msg = null;
                try {
                    msg = finalSession.createTextMessage(body);
                    row = table.getSelectedRow();
                    text = table.getValueAt(row,0).toString();
                    for (int i = 0;i < messages.size();i++){
                        String c = ((TextMessage) messages.get(i)).getText();
                        Student j = gson.fromJson(c,Student.class);
                        if (j.getName().equals(text)){
                            msg.setJMSCorrelationID(messages.get(i).getJMSMessageID());
                        }
                    }
                    // send the message
                    producer.send(finalDestination,msg);
                    // print all message attributes; but JMSDestination is null
                    // session makes the message via ActiveMQ. AtiveMQ assigns unique JMSMessageID
                    // to each message.
                    //print all message attributes; but JMSDestination is senderDestination name
                    System.out.println(msg);
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }


                try {
                    //print only the attributes you want to see
                    System.out.println("JMSMessageID=" + msg.getJMSMessageID()
                            + " JMSDestination=" + msg.getJMSDestination()
                            + " Text=" + ((TextMessage) msg).getText());
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            }
        });


        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(450,250));
        jp.add(scrollPane, BorderLayout.CENTER);

        jp.setSize(400,250);
        frame.add(jp, BorderLayout.CENTER);
        frame.setSize(500, 350);
        frame.setVisible(true);
    }


}
