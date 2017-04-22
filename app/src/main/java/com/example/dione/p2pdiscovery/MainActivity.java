package com.example.dione.p2pdiscovery;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.ListViewCompat;
import android.util.Log;
import android.util.MutableChar;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ch.uepaa.p2pkit.P2PKitClient;
import ch.uepaa.p2pkit.P2PKitStatusCallback;
import ch.uepaa.p2pkit.StatusResult;
import ch.uepaa.p2pkit.StatusResultHandling;
import ch.uepaa.p2pkit.discovery.GeoListener;
import ch.uepaa.p2pkit.discovery.InfoTooLongException;
import ch.uepaa.p2pkit.discovery.P2PListener;
import ch.uepaa.p2pkit.discovery.entity.Peer;
import ch.uepaa.p2pkit.internal.messaging.MessageTooLargeException;
import ch.uepaa.p2pkit.messaging.MessageListener;
import com.example.dione.p2pdiscovery.ciphers;
import com.example.dione.p2pdiscovery.encryption.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static android.R.attr.publicKey;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String APP_KEY = "9a97db9011d04bd89442ee46640a7feb";
    private Context mContext;
    private Button sendMessagetoPeers;
    ArrayList<Peer> nodeList;
    PeerAdapter peerAdapter;
    ListViewCompat peerList;
    AppCompatTextView myNodeId;
    AppCompatTextView noPeersAroundMessage;
    ProgressDialog progressDialog;
    EditText msg;
    EditText receiver;
    Peer a[]=new Peer[10];
    private static  final int uniqueID=54312;
    NotificationCompat.Builder notification;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        KeyPairGenerator kpg;
        KeyPair kp;
        PublicKey publicKey;
        PrivateKey privateKey;
        byte [] encryptedBytes,decryptedBytes;
        Cipher cipher,cipher1;
        String encrypted,decrypted;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        initViews();
        enableP2PKit();
        initPeerAdapter();
        WifiManager wifiManager = (WifiManager)this.mContext.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        /*kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(16);
        kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();*/
    }

    private void initViews(){
        progressDialog = new ProgressDialog(mContext);
        sendMessagetoPeers = (Button) findViewById(R.id.sendMessagetoPeers);
        sendMessagetoPeers.setOnClickListener(this);
        myNodeId = (AppCompatTextView) findViewById(R.id.myNodeId);
        noPeersAroundMessage = (AppCompatTextView) findViewById(R.id.noPeersAroundMessage);
        nodeList = new ArrayList<>();
    }

    private void showNoPeerMessage(){
        peerList.setVisibility(View.GONE);
        noPeersAroundMessage.setVisibility(View.VISIBLE);
    }

    private void showPeerList(){
        peerList.setVisibility(View.VISIBLE);
        noPeersAroundMessage.setVisibility(View.GONE);
    }
    private void initPeerAdapter(){
        peerList = (ListViewCompat) findViewById(R.id.peerList);
        peerAdapter = new PeerAdapter(mContext, nodeList);
        peerList.setAdapter(peerAdapter);
        peerAdapter.notifyDataSetChanged();
    }
    private void enableP2PKit(){
         final P2PKitStatusCallback mStatusCallback = new P2PKitStatusCallback() {
            @Override
            public void onEnabled() {
                // ready to start discovery
                addP2PListener();
                addGeoDiscovery();
                addMessaging();
                progressDialog.dismiss();
            }

            @Override
            public void onSuspended() {
                // p2pkit is temporarily suspended
            }

            @Override
            public void onResumed() {
                // coming back from a suspended state
            }

            @Override
            public void onDisabled() {
                // p2pkit has been disabled
            }

            @Override
            public void onError(StatusResult statusResult) {
                // enabling failed, handle statusResult
            }
        };

        final StatusResult result = P2PKitClient.isP2PServicesAvailable(this);
        if (result.getStatusCode() == StatusResult.SUCCESS) {
            P2PKitClient client = P2PKitClient.getInstance(this);
            client.enableP2PKit(mStatusCallback, APP_KEY);
            progressDialog.setMessage("Enabling P2P Kit");
            progressDialog.setCancelable(true);
            progressDialog.show();
        } else {
            StatusResultHandling.showAlertDialogForStatusError(this, result);
        }
    }

    private void addP2PListener(){
        final P2PListener mP2pDiscoveryListener = new P2PListener() {
            @Override
            public void onP2PStateChanged(int state) {
                Log.d("P2PListener", "State changed: " + state);
            }

            @Override
            public void onPeerDiscovered(Peer peer) {
                Log.d("P2PListener", "Peer discovered: " + peer.getNodeId() );
                if (!nodeList.contains(peer)){
                    showPeerList();
                    nodeList.add(peer);
                    peerAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onPeerLost(Peer peer) {
                Log.d("P2PListener", "Peer lost: " + peer.getNodeId());
                nodeList.remove(peer);
                peerAdapter.notifyDataSetChanged();
                if (nodeList.isEmpty()){
                    showNoPeerMessage();
                }
            }

            @Override
            public void onPeerUpdatedDiscoveryInfo(Peer peer) {
                Log.d("P2PListener", "Peer updated: " + peer.getNodeId() + " with new info: " + new String(peer.getDiscoveryInfo()));
            }

            @Override
            public void onProximityStrengthChanged(Peer peer) {
                Log.d("P2pListener", "Peer " + peer.getNodeId() + " changed proximity strength: " + peer.getProximityStrength());
            }
        };
        myNodeId.setText("My Id-" +String.valueOf(P2PKitClient.getInstance(this).getNodeId()));
        P2PKitClient.getInstance(mContext).getDiscoveryServices().addP2pListener(mP2pDiscoveryListener);

        try {
            P2PKitClient.getInstance(this).getDiscoveryServices().setP2pDiscoveryInfo("Hello p2pkit".getBytes());
        } catch (InfoTooLongException e) {
            Log.e("P2PListener", "The discovery info is too long");
        }
    }

    private void addGeoDiscovery(){
        final GeoListener mGeoDiscoveryListener = new GeoListener() {
            @Override
            public void onGeoStateChanged(int state) {
                Log.d("GeoListener", "State changed: " + state);
            }

            @Override
            public void onPeerDiscovered(final UUID nodeId) {
                Log.d("GeoListener", "Peer discovered: " + nodeId);
//                if (!nodeList.contains(nodeId)) {
//                    nodeList.add(nodeId);
//                }
            }

            @Override
            public void onPeerLost(final UUID nodeId) {
                Log.d("GeoListener", "Peer lost: " + nodeId);
            }
        };
        P2PKitClient.getInstance(mContext).getDiscoveryServices().addGeoListener(mGeoDiscoveryListener);
    }

    private void addMessaging(){
        final MessageListener mMessageListener = new MessageListener() {
            @Override
            public void onMessageStateChanged(int state) {
                Log.d("MessageListener", "State changed: " + state);
            }

            public void notify(CharSequence x)
            {
                notification=new NotificationCompat.Builder(MainActivity.this);
               // setContentView(R.layout.activity_main);
                notification.setSmallIcon(R.drawable.pockets);
                notification.setTicker("You have received a new message");
                notification.setWhen(System.currentTimeMillis());
                notification.setContentTitle("Pockets");
                notification.setContentText(x);
                
                Intent intent=new Intent(MainActivity.this,MainActivity.class);
                TaskStackBuilder stackBuilder=TaskStackBuilder.create(MainActivity.this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(intent);

                PendingIntent pi= stackBuilder.getPendingIntent(uniqueID,PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setContentIntent(pi);

                NotificationManager nm=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.notify(uniqueID,notification.build());


            }

            @Override
            public void onMessageReceived(long timestamp, UUID origin, String type, byte[] message){
                    String oldmsg="";
                    try {

                        rc2 r = new rc2();
                        byte[] decrypted = r.process(message,"-d");
                        String plain = new String(decrypted);



                        notify(new String(decrypted));

                        Toast.makeText(mContext, origin + ":\nemessage=" + new String(decrypted), Toast.LENGTH_LONG).show();
                    }catch(Exception java){
                        Toast.makeText(mContext, "Error while decrypting the cipher text", Toast.LENGTH_LONG).show();
                    }

                    //String plain = new String(decrypted);
                    //ciphers c = new ciphers();
                    //String plain = c.RSADecrypt(message);
                    //String cipherText = new String(message);
                    //String plainText = ciphers.RSADecrypt(message);
                    //receiver = (EditText) findViewById(R.id.sendTo);
                    //(!oldmsg.equals(plain))
                    //Toast.makeText(mContext, origin + ":\nemessage=" + new String(message), Toast.LENGTH_LONG).show();
                    //oldmsg=plain;

                    //needs testing
                    //UUID sender = P2PKitClient.getInstance(mContext).getNodeId();

                    //relaying the received message to other nodes
                    //P2PKitClient.getInstance(mContext).getMessageServices().sendMessage(sender, "text/plain", message
            }
        };
        P2PKitClient.getInstance(mContext).getMessageServices().addMessageListener(mMessageListener);
    }

    private void sendMessage(UUID nodeId){
        try {
            ciphers c = new ciphers();
            rc2 r=new rc2();
            msg= (EditText) findViewById(R.id.message);
            String input= msg.getText().toString();
            byte[] plainText = input.getBytes();
            byte[] cipherText = r.process(plainText,"-e");
            //byte[] toEncrypt=input.getBytes();

            //encrypting usnig RSA
            //byte[] encrypted = c.RSAEncrypt(input);
            //String toSend = new String (encrypted);

            //String cipherText = rc2.encrypt(input,"password");



            P2PKitClient.getInstance(mContext).getMessageServices().sendMessage(nodeId, "text/plain", cipherText);
            Toast.makeText(mContext, "Sending message to " + String.valueOf(nodeId), Toast.LENGTH_SHORT).show();

        } catch (MessageTooLargeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.sendMessagetoPeers:

                if (nodeList != null){
                    AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                    View mView=getLayoutInflater().inflate(R.layout.popup,null);
                    RadioButton opt1=(RadioButton)mView.findViewById(R.id.radioButton);
                    RadioButton opt2=(RadioButton)mView.findViewById(R.id.radioButton1);
                    RadioButton opt3=(RadioButton)mView.findViewById(R.id.radioButton2);
                    RadioButton opt4=(RadioButton)mView.findViewById(R.id.radioButton3);

                    int i=0;
                    for(Peer nodes: nodeList)
                    {
                        a[i]=nodes;
                        i++;

                    }
                    opt1.setOnClickListener(new View.OnClickListener(){

                        public void onClick(View view)
                        {
                          sendMessage(a[0].getNodeId());
                        }
                    });
                    opt2.setOnClickListener(new View.OnClickListener(){

                        public void onClick(View view)
                        {
                            sendMessage(a[1].getNodeId());
                        }
                    });
                    opt3.setOnClickListener(new View.OnClickListener(){

                        public void onClick(View view)
                        {
                            sendMessage(a[2].getNodeId());
                        }
                    });
                    opt4.setOnClickListener(new View.OnClickListener(){

                        public void onClick(View view)
                        {
                            for (Peer nodes: nodeList) {
                                sendMessage(nodes.getNodeId());
                            }
                        }
                    });
                    builder.setView(mView);
                    AlertDialog dialog=builder.create();
                    dialog.show();




                    /*
                    for (Peer nodes: nodeList) {
                        sendMessage(nodes.getNodeId());
                    }
                */

                }
                break;
        }
    }
}
