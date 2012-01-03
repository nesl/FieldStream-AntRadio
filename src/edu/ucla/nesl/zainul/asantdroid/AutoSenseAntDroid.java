package edu.ucla.nesl.zainul.asantdroid;


import org.fieldstream.service.sensors.mote.MoteSensorManager;
import org.fieldstream.service.sensors.mote.tinyos.ChannelToSensorMapping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.os.Bundle;
import android.util.Log;
//import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.exception.*;
import com.dsi.ant.AntInterface;
import com.dsi.ant.AntInterfaceIntent;
import com.dsi.ant.AntMesg;
import com.dsi.ant.AntDefine;
import com.dsi.ant.R;


public class AutoSenseAntDroid {

	   /** The Log Tag. */
	   public static final String TAG = "AutoSenseApp";   

	   /** The interface to the ANT radio. */
	   public static AntInterface sAntReceiver;
	   
	   /** Is the ANT background service connected. */
	   private boolean mServiceConnected = false;
	   
	   /** Flag to know if the ANT App was interrupted. */
	   private boolean mAntInterrupted = false;

	   /** Flag to know if we have requested ANT to be enabled, and are waiting for ANT_ENABLED. */
	   private boolean mEnabling = false;

	   private Context mContext;
	   
	   /** The key for referencing the interrupted variable in saved instance data. */
	   static final String ANT_INTERRUPTED_KEY = "ant_interrupted";
	   
	   /** The key for referencing the state variable in saved instance data. */
	   static final String ANT_STATE_KEY = "ant_state";

	   /** Stores which ANT status Intents to receive. */
	   private IntentFilter statusIntentFilter;

	   /** Stores which ANT message Intents to receive. */
	   private IntentFilter messageIntentFilter;
	   
	   /** If this application has control of the ANT Interface. */
	   private boolean mClaimedAntInterface;

	   /** Is the Intent {@link BroadcastReceiver} unregistered. */
	   private boolean mReceiverDisabled = true;
	   
	   /** Flag to know if an ANT Reset was triggered by this application. */
	   private boolean mAntResetSent = false;
	   
	   /** Pair to any device. */
	   private static final short WILDCARD = 0;
	   
	   /** The ANT channel for the AutoSense. */
	   static final byte AutoSense_CHANNEL = (byte) 0;
	   
	   /** The default proximity search bin. */
	   private static final byte DEFAULT_BIN = 7;
	   
	   /** The default event buffering buffer threshold. */
	   private static final short DEFAULT_BUFFER_THRESHOLD = 0;

	   /** AutoSense device number. */
	   public short mDeviceNumberAutoSense;
	   
	   /** Devices must be within this bin to be found during (proximity) search. */
	   public byte mProximityThreshold;
	   
	   /** Data buffered for event buffering before flush. */
	   public short mBufferThreshold;
	   
	   /** Whether to process ANT data. */
	   private boolean mAntProcessingEnabled = true;
	   
	   /** The ECG channel for AutoSense. */
	   static final byte ECG_CHANNEL = (byte) 0;
	   
	   /** The ACCELX channel for AutoSense. */
	   static final byte ACCELX_CHANNEL = (byte) 1;
	   
	   /** The ACCELY channel for AutoSense. */
	   static final byte ACCELY_CHANNEL = (byte) 2;
	   
	   /** The ACCELZ channel for AutoSense. */
	   static final byte ACCELZ_CHANNEL = (byte) 3;
	   
	   /** The GSR channel for AutoSense. */
	   static final byte GSR_CHANNEL = (byte) 4;
	   
	   /** The RIP channel for AutoSense. */
	   static final byte RIP_CHANNEL = (byte) 7;
	   
	   /** The SKIN, AMBIENCE, BATTERY channels for AutoSense. */
	   static final byte MISC_CHANNEL = (byte) 8;
	   
	   /** ECG text label. */	   
	   //private TextView textECG;
	   
	   /** ACCELX text label. */	   
	   //private TextView textACCELX;

	   /** ACCELY text label. */	   
	   //private TextView textACCELY;

	   /** ACCELZ text label. */	   
	   //private TextView textACCELZ;

	   /** GSR text label. */	   
	   //private TextView textGSR;

	   /** SKIN text label. */	   
	   //private TextView textSKIN;

	   /** AMBIENT text label. */	   
	   //private TextView textAMBIENT;

	   /** RIP text label. */	   
	   //private TextView textRIP;

	   /** BATTERY text label. */	   
	   //private TextView textBATTERY;

	   /** STATUS text label. */	   
	   //private TextView textSTATUS;

	   /** Packet reception statistics */
	   private int pktsReceived = 0;
	   
	   /** Packet drop statistics */
	   private int pktsDropped = 0;
	   
	   /** To compute dropped packets */
	   private byte mLastSequenceNumber = 0;
	   
	   /**
	    * Class for receiving notifications about ANT service state.
	    */
	   private AntInterface.ServiceListener mAntServiceListener = new AntInterface.ServiceListener() 
	   { 
	           public void onServiceConnected() 
	           {
	               Log.d(TAG, "mAntServiceListener onServiceConnected()");
	               
	               mServiceConnected = true;
	               
	               // Need to enable the ANT radio, now the service is connected
	               if(mAntInterrupted == false)
	               {
	                   try
	                   {
	                       if(sAntReceiver.claimInterface())
	                       {
	                           if(!enableRadio())
	                           {
	                               Log.i(TAG, "mAntServiceListener onServiceConnected: ANT radio could not be enabled");
	                           }
	                       }
	                   }
	                   catch(AntInterfaceException e)
	                   {
                           Log.e(TAG, "mAntServiceListener: Exception caught");
	                   }
	               }
	               else
	               {
	                   Log.i(TAG,"Not attempting to enable radio as application was interrupted (leaving in previous state).");
	               }

	               connectAutoSense();
	               Log.d(TAG, "mAntServiceListener: Connecting to AutoSense");
	           }

	           public void onServiceDisconnected() 
	           {
	               Log.d(TAG, "mAntServiceListener onServiceDisconnected()");
	               
	               mServiceConnected = false;
	           }
	       };

	
	
	/** Called when the activity is first created. */
    //@Override
    //public void onCreate(Bundle savedInstanceState) {
    //    super.onCreate(savedInstanceState);
	public AutoSenseAntDroid(Context context) {
		
        Log.d(TAG, "onCreate enter");              
        
        /*Retrieve the ANT state and find out whether the ANT App was interrupted*/
        /*if (savedInstanceState != null) 
        {
           Bundle antState = savedInstanceState.getBundle(ANT_STATE_KEY);
           if (antState != null) 
           {
               mAntInterrupted = antState.getBoolean(ANT_INTERRUPTED_KEY, false);       
           }
        }*/
       
        mClaimedAntInterface = false;
        
        // ANT intent broadcasts.
        statusIntentFilter = new IntentFilter();
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_ENABLED_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_DISABLED_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_RESET_ACTION);
        statusIntentFilter.addAction(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION);
        
        messageIntentFilter = new IntentFilter();
        messageIntentFilter.addAction(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION);

        //Context context = this.getApplicationContext();
        mContext = context;
        
        //sAntReceiver = (AntInterface) getLastNonConfigurationInstance();
        //if (sAntReceiver == null) 
        //{
            sAntReceiver = AntInterface.getInstance(context, mAntServiceListener);
        //}

        if(null == sAntReceiver)
        {
            if(AntInterface.hasAntSupport(context))
            {
                Toast installNotification = Toast.makeText(context, context.getResources().getString(R.string.Notify_Service_Required), Toast.LENGTH_LONG);
                installNotification.show();
            
                AntInterface.goToMarket(context);
            
                // There is no point running this application without ANT support.
                Log.e(TAG, "onCreate: No ANT Service, quitting.");
                //finish();
            }
            else
            {
                Log.i(TAG, "onCreate: ANT not supported.");
            }
        }
        else
        {
            mServiceConnected = sAntReceiver.isServiceConnected();
            
            context.registerReceiver(mAntStatusReceiver, statusIntentFilter);
            mReceiverDisabled = false;
        }

        //setContentView(R.layout.main);


        /* Set up all the text views */
        /*textECG = new TextView(this);
        textECG = (TextView) findViewById(R.id.textViewECG);
        
        textACCELX = new TextView(this);
        textACCELX = (TextView) findViewById(R.id.textViewACCELX);

        textACCELY = new TextView(this);
        textACCELY = (TextView) findViewById(R.id.textViewACCELY);

        textACCELZ = new TextView(this);
        textACCELZ = (TextView) findViewById(R.id.textViewACCELZ);

        textGSR = new TextView(this);
        textGSR = (TextView) findViewById(R.id.textViewGSR);

        textSKIN = new TextView(this);
        textSKIN = (TextView) findViewById(R.id.textViewSKIN);

        textAMBIENT = new TextView(this);
        textAMBIENT = (TextView) findViewById(R.id.textViewAMBIENT);

        textRIP = new TextView(this);
        textRIP = (TextView) findViewById(R.id.textViewRIP);

        textBATTERY = new TextView(this);
        textBATTERY = (TextView) findViewById(R.id.textViewBATTERY);

        textSTATUS = new TextView(this);
        textSTATUS = (TextView) findViewById(R.id.textViewSTATUS);*/

        loadDefaultConfiguration();

        
        Log.d(TAG, "onCreate exit");
    }
    
    private void connectAutoSense()
    {
        {
            Log.d(TAG, "onClick: No channels open, reseting ANT");
            mAntResetSent = true;
            
            try
            {
               sAntReceiver.ANTResetSystem();
               setAntConfiguration();
            }
            catch(AntInterfaceException e)
            {
                Log.e(TAG, "onClick: Could not reset ANT", e);
                mAntResetSent = false;
            }
         }


        /* Set up a connection to AutoSense */
        if(!antChannelSetup(
        		(byte) 0x00,        // Network:              0 (ANT)
                (byte) 0x00,        // Channel Number 0
                (byte) 0xFFFF,      // Device Number
                (byte) 0x01,        // Device Type:          
                (byte) 0x00,        // Transmission Type:    wild card
                (short) 0x04EC,     // Channel period:       1260 (=26.00Hz)
                (byte) 0x50,        // RF Frequency:         80 (AutoSense)
                mProximityThreshold))
        {
        	Log.w(TAG, "onClick Open channel: Channel Setup failed");
        }
        

    }
    
    /**
     * Retrieve application persistent data.
     */
    private void loadDefaultConfiguration()
    {
       mDeviceNumberAutoSense = (short) (WILDCARD);
       mProximityThreshold = (byte) (DEFAULT_BIN);
       mBufferThreshold = (short) (DEFAULT_BUFFER_THRESHOLD);
    }

    /**
     * Configure the ANT radio to the user settings.
     */
    private void setAntConfiguration()
    {
        if(mServiceConnected)
        {
            try
            {
                // Event Buffering Configuration
                if(mBufferThreshold > 0)
                {
                    //TODO For easy demonstration will set screen on and screen off thresholds to the same value.
                    // No buffering by interval here.
                    sAntReceiver.ANTConfigEventBuffering((short)0xFFFF, mBufferThreshold, (short)0xFFFF, mBufferThreshold);
                }
                else
                {
                    sAntReceiver.ANTDisableEventBuffering();
                }
            }
            catch(AntInterfaceException e)
            {
                Log.e(TAG, "Could not configure event buffering", e);
            }
        }
    }
    

    /** Receives all of the ANT status intents. */
    private final BroadcastReceiver mAntStatusReceiver = new BroadcastReceiver() 
    {      

       public void onReceive(Context context, Intent intent) 
       {
          String ANTAction = intent.getAction();

          Log.d(TAG, "enter onReceive: " + ANTAction);
          if (ANTAction.equals(AntInterfaceIntent.ANT_ENABLED_ACTION)) 
          {
             Log.i(TAG, "onReceive: ANT ENABLED");
             
             mEnabling = false;
          }
          else if (ANTAction.equals(AntInterfaceIntent.ANT_DISABLED_ACTION)) 
          {
             Log.i(TAG, "onReceive: ANT DISABLED");
             
             mEnabling = false;
             
          }
          else if (ANTAction.equals(AntInterfaceIntent.ANT_RESET_ACTION))
          {
             Log.d(TAG, "onReceive: ANT RESET");
             
             if(false == mAntResetSent)
             {
                //Someone else triggered an ANT reset
                Log.d(TAG, "onReceive: ANT RESET: Resetting state");
                
             }
             else
             {
                mAntResetSent = false;
             }
          }
          else if (ANTAction.equals(AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION)) 
          {
             Log.i(TAG, "onReceive: ANT INTERFACE CLAIMED");
             
             boolean wasClaimed = mClaimedAntInterface;
             
             // Could also read ANT_INTERFACE_CLAIMED_PID from intent and see if it matches the current process PID.
             try
             {
                 mClaimedAntInterface = sAntReceiver.hasClaimedInterface();

                 if(wasClaimed != mClaimedAntInterface)
                 {
                     if(mClaimedAntInterface)
                     {
                         Log.i(TAG, "onReceive: ANT Interface claimed");

                         mContext.registerReceiver(mAntMessageReceiver, messageIntentFilter);
                     }
                     else
                     {
                         Log.i(TAG, "onReceive: ANT Interface released");

                         mContext.unregisterReceiver(mAntMessageReceiver);

                     }
                 }
             }
             catch(AntInterfaceException e)
             {
                 Log.e(TAG, "onReceive: Exeption caught");
             }
          }
       }
    };
    
    /** Receives all of the ANT message intents and dispatches to the proper handler. */
    private final BroadcastReceiver mAntMessageReceiver = new BroadcastReceiver() 
    {      

       public void onReceive(Context context, Intent intent) 
       {
    	   long timestamp = System.currentTimeMillis();
    	   
          String ANTAction = intent.getAction();

          Log.d(TAG, "enter onReceive: " + ANTAction);
          if (ANTAction.equals(AntInterfaceIntent.ANT_RX_MESSAGE_ACTION)) 
          {
             Log.d(TAG, "onReceive: ANT RX MESSAGE");

             byte[] ANTRxMessage = intent.getByteArrayExtra(AntInterfaceIntent.ANT_MESSAGE);
             String text = "Rx:";
          
             for(int i = 0;i < ANTRxMessage.length; i++)
                text += "[" + Integer.toHexString((int) ANTRxMessage[i] & 0xFF) + "]";
          
             Log.i(TAG, text);
             
             byte rxChannelNumber = (byte)(ANTRxMessage[AntMesg.MESG_DATA_OFFSET] & AntDefine.CHANNEL_NUMBER_MASK);
             switch(rxChannelNumber)   // Parse channel number
             {
             case AutoSense_CHANNEL:
                 antDecodeAutoSense(ANTRxMessage, timestamp);
                 break;
               default:
                   Log.i(TAG, "onReceive: Message for different channel ("+ rxChannelNumber +")");
                   break;
             }
          }
       }
       
       /**
        * Decode AutoSense messages.
        *
        * @param ANTRxMessage the received ANT message.
        */
       private void antDecodeAutoSense(byte[] ANTRxMessage, long timestamp)
       {
    	  final byte[] fixed_send_order = {0,1,0,2,0,7,0,3,0,4,0,7,(byte)0xFF,(byte)0xFF,(byte)0xFF,8};
    	  
          Log.d(TAG, "antDecodeAutoSense start");
           
          if(mAntProcessingEnabled && ANTRxMessage[AntMesg.MESG_ID_OFFSET] == AntMesg.MESG_BROADCAST_DATA_ID)   // message ID == broadcast
          {
             Log.d(TAG, "antDecodeAutoSense: Received broadcast");

             if(mDeviceNumberAutoSense == WILDCARD)
             {
                 try
                 {
                     //Log.i(TAG, "antDecodeAutoSense: Requesting device number");

                     sAntReceiver.ANTRequestMessage(AutoSense_CHANNEL, AntMesg.MESG_CHANNEL_ID_ID);
                 }
                 catch(AntInterfaceException e)
                 {
                     Log.e(TAG, "antDecodeAutoSense: Exception caught " + e);
                 }
             }
             
             
             byte mSequenceNumber = (byte)(ANTRxMessage[AntMesg.MESG_DATA_OFFSET+8] & 0x0F);
             byte mChannelNumber = fixed_send_order[mSequenceNumber];
             Log.d(TAG, "antDecodeAutoSense: Sequence Number " + mSequenceNumber + " Channel: " + mChannelNumber);
             int[] samples = decodeAutoSenseSamples(ANTRxMessage);
             
             pktsReceived++;
             /* Compute dropped packet statistics on the primary channels only */
             if (mSequenceNumber < 12) {
            	 pktsDropped += (11 + mSequenceNumber - mLastSequenceNumber) % 12;
                 mLastSequenceNumber = mSequenceNumber;
             }
             
      	     //try {
    	    	 //textSTATUS.setText("Received: " + pktsReceived + " Dropped: " + pktsDropped);
    	    	 Log.d(TAG, "Received: " + pktsReceived + " Dropped: " + pktsDropped);
    	     //}
    	     //catch(Exception e)
     	     //{
    	    	//Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
     	     //}

    	    MoteSensorManager msm = MoteSensorManager.getInstance();
    	                 
             switch(fixed_send_order[mSequenceNumber])
             {
             case ECG_CHANNEL:
	         	    //try {
	        	    	//textECG.setText("ECG: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	         	    	Log.d(TAG, "ECG: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
         	    	msm.updateSensor(samples, 1, ChannelToSensorMapping.ECG, timestamp);
            	 break;
             case ACCELX_CHANNEL:
            	    //try {
	        	    	//textACCELX.setText("AccX: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
            	    	Log.d(TAG, "AccX: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
            	    //}
            	    //catch(Exception e)
            	    //{
            	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
            	    //}
            	    msm.updateSensor(samples, 1, ChannelToSensorMapping.ACCELX, timestamp);
            	 break;
             case ACCELY_CHANNEL:
	         	    //try {
	        	    	//textACCELY.setText("AccY: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	         	    	//Log.d(TAG, "AccY: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
	         	    	msm.updateSensor(samples, 1, ChannelToSensorMapping.ACCELY, timestamp);
            	 break;
             case ACCELZ_CHANNEL:
	         	    //try {
	        	    	//textACCELZ.setText("AccZ: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	         	    	//Log.d(TAG, "AccZ: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    	//Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
         	 		//Log.d(TAG, "ACCELZ: " + samples[0]);
	         	    	msm.updateSensor(samples, 1, ChannelToSensorMapping.ACCELZ, timestamp);
            	 break;
             case GSR_CHANNEL:
	         	    //try {
	        	    	//textGSR.setText("GSR: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	         	    	//Log.d(TAG, "GSR: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
         	 		//Log.d(TAG, "GSR: " + samples[0]);
	         	    	msm.updateSensor(samples, 1, ChannelToSensorMapping.GSR, timestamp);
            	 break;
             case RIP_CHANNEL:
	         	    //try {
	        	    	//textRIP.setText("RIP: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	         	    	//Log.d(TAG, "RIP: " + samples[0] + " " + samples[1] + " " + samples[2] + " " + samples[3] + " " + samples[4]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
         	 		//Log.d(TAG, "RIP: " + samples[0]);
	         	    	msm.updateSensor(samples, 1, ChannelToSensorMapping.RIP, timestamp);
            	 break;
             case MISC_CHANNEL:
	         	    //try {
	        	    	//textBATTERY.setText("Battery: " + (float)samples[0]/4096*3*2 + "V");
	        	    	//textSKIN.setText("Skin Temperature: " + samples[1]);
	        	    	//textAMBIENT.setText("Ambient Temperature: " + samples[2]);
	        	    	//Log.d(TAG, "Battery: " + (float)samples[0]/4096*3*2 + "V");
	        	    	//Log.d(TAG, "Skin Temperature: " + samples[1]);
	        	    	//Log.d(TAG, "Ambient Temperature: " + samples[2]);
	        	    //}
	        	    //catch(Exception e)
	        	    //{
	        	    //	Log.e(TAG, "antDecodeAutoSense: Caught exception " + e);
	        	    //}
         	 		//Log.d(TAG, "SKIN TEMP: " + samples[1]);
         	 		//Log.d(TAG, "AMBIENT TEMP: " + samples[2]);
         	 		//Log.d(TAG, "BATTERY: " + samples[0]);
            	 break;
            	 default:
            		 Log.w(TAG, "Unknown channel " + mChannelNumber);
             }
             
             
          }         
          else if(ANTRxMessage[AntMesg.MESG_ID_OFFSET]==AntMesg.MESG_RESPONSE_EVENT_ID && ANTRxMessage[3]==AntMesg.MESG_EVENT_ID && ANTRxMessage[4]==AntDefine.EVENT_RX_SEARCH_TIMEOUT)   // Search timeout
          {
              try
              {
                  Log.i(TAG, "antDecodeWeight: Received search timeout");

                  sAntReceiver.ANTUnassignChannel(AutoSense_CHANNEL);
              }
              catch(AntInterfaceException e)
              {
                  Log.e(TAG, "antDecodeAutoSense: Exception caught " + e);
              }
          }    
          else if(ANTRxMessage[AntMesg.MESG_ID_OFFSET]==AntMesg.MESG_CHANNEL_ID_ID)   // Store requested Channel Id
          {
             Log.i(TAG, "antDecodeWeight: Received device number");

             mDeviceNumberAutoSense = (short) (((int)ANTRxMessage[3]&0xFF  | ((int)(ANTRxMessage[4]&0xFF) << 8)) & 0xFFFF);
          }
          
          Log.d(TAG, "antDecodeWeight end");
       }

 
    };
    
    /**
     * Decode AutoSense samples.
     *
     * @param ANTRxMessage the received ANT message.
     */

    private int[] decodeAutoSenseSamples(byte[] ANTRxMessage)
    {
        int[] samples = new int[5];
        /* Decode 5 samples of 12 bits each */
        samples[0] = (short)(( (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+1] & 0x00FF) << 4) | (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+2] & 0x00FF) >>> 4) ) & 0x0FFF);
        samples[1] = (short)(( (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+2] & 0x00FF) << 8) | ((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+3] & 0x00FF) ) & 0x0FFF);
        samples[2] = (short)(( (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+4] & 0x00FF) << 4) | (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+5] & 0x00FF) >>> 4) ) & 0x0FFF);
        samples[3] = (short)(( (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+5] & 0x00FF) << 8) | ((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+6] & 0x00FF) ) & 0x0FFF);
        samples[4] = (short)(( (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+7] & 0x00FF) << 4) | (((short)ANTRxMessage[AntMesg.MESG_DATA_OFFSET+8] & 0x00FF) >>> 4) ) & 0x0FFF);
        
        return samples;
    }
    
    /**
     * ANT Channel Configuration.
     *
     * @param networkNumber the network number
     * @param channelNumber the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @param channelPeriod the channel period
     * @param radioFreq the radio freq
     * @param proxSearch the prox search
     * @return true, if successfully configured and opened channel
     */   
    private boolean antChannelSetup(byte networkNumber, byte channelNumber, short deviceNumber, byte deviceType, byte txType, short channelPeriod, byte radioFreq, byte proxSearch)
    {
       boolean channelOpen = false;

       try
       {
           sAntReceiver.ANTAssignChannel(channelNumber, AntDefine.PARAMETER_RX_NOT_TX, networkNumber);  // Assign as slave channel on selected network (0 = public, 1 = ANT+, 2 = ANTFS)
           sAntReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType, txType);
           sAntReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod);
           sAntReceiver.ANTSetChannelRFFreq(channelNumber, radioFreq);
           sAntReceiver.ANTSetChannelSearchTimeout(channelNumber, (byte)0); // Disable high priority search
           sAntReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber,(byte) 12); // Set search timeout to 30 seconds (low priority search)
           
           if(deviceNumber == WILDCARD)
           {
               sAntReceiver.ANTSetProximitySearch(channelNumber, proxSearch);   // Configure proximity search, if using wild card search
           }

           sAntReceiver.ANTOpenChannel(channelNumber);
           
           channelOpen = true;
       }
       catch(AntInterfaceException aie)
       {
    	   Log.e(TAG, "antChannelSetup: Exception caught");
       }
      
	   Log.d(TAG, "antChannelSetup: Channel Open");
       return channelOpen;
    }


    /**
     * Enable radio.
     *
     * @return true, if successfully sent enable request to radio.
     */
    private boolean enableRadio()
    {
        boolean result = false;
        
        if(!AntInterface.hasAntSupport(mContext.getApplicationContext()))
        {
            Log.w(TAG, "enableRadio: ANT not supported");

            //mAntStateText.setText(R.string.Text_ANT_Not_Supported);
        }
        else
        {
            if(mServiceConnected)
            {
                try
                {
                    if(sAntReceiver.isEnabled())
                    {
                        result = true;
                    }
                    else
                    {
                        sAntReceiver.enable();
                        mEnabling = true;
                        result = true;
                    }
                }
                catch(AntInterfaceException e)
                {
                    Log.e(TAG, "enableRadio: Exception caught");
                }
            }
            else
            {
                Log.w(TAG, "enableRadio: Could not enable, ANT service not connected");
            }
        }

        return result;
    }

    //@Override 
    //protected void onDestroy()
    public void disableRadio()
    {
       Log.d(TAG, "onDestroy enter");

       try
       {
           sAntReceiver.ANTCloseChannel(AutoSense_CHANNEL);
           sAntReceiver.ANTUnassignChannel(AutoSense_CHANNEL);
       }
       catch(AntInterfaceException e)
       {
           Log.w(TAG, "Exception in onDestroy", e);
       }

       if(!mReceiverDisabled)
       {
          mContext.unregisterReceiver(mAntStatusReceiver);
          mReceiverDisabled = true;
       }
       
       if(mClaimedAntInterface)
       {
          mContext.unregisterReceiver(mAntMessageReceiver);
       }
       
       try
       {
          //if(isFinishing())
          {
             Log.d(TAG, "onDestroy: isFinishing");
           
             if(mServiceConnected)
             {
                if(mClaimedAntInterface)
                {
                   Log.d(TAG, "onDestroy: Releasing interface");
                
                   sAntReceiver.releaseInterface();
                }
                
                sAntReceiver.stopRequestForceClaimInterface();
                
                sAntReceiver.destroy();
             }
          }
       }
       catch(Exception e)
       {
          Log.w(TAG, "Exception in onDestroy", e);
       }
       
       statusIntentFilter = null;
       messageIntentFilter = null;

       //super.onDestroy();

       Log.d(TAG, "onDestroy exit");
    }   


}
