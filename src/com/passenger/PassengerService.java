package com.passenger;

import java.util.HashMap;
import java.util.Locale;

import com.passenger.R;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Passenger Service
 * 
 * The Passenger Service runs in the background and, when enabled,
 * parses SMS Broadcasts and speaks them aloud using TextToSpeech.
 * 
 * @author Justin C. Appler
 * @version 02/11/2011
 */
public class PassengerService extends Service implements OnInitListener
{  
   private static TextToSpeech m_TextToSpeech = null;
   private static boolean m_TTSInitialized = false;
   private final IBinder m_Binder = new LocalBinder();

   private static Looper m_ServiceLooper;
   private static SMSHandler m_SMSHandler;
   
   private final class SMSHandler extends Handler 
   {
      public SMSHandler(Looper looper)
      {
         super(looper);
      }

      @Override
      public void handleMessage(Message message) 
      {
          Bundle intentExtras = (Bundle) message.obj;

          SmsMessage[] messages = getMessagesFromBundle(intentExtras);
          
          if (m_TextToSpeech != null)
          {
             speakMessages(getApplicationContext(), messages);
             Log.i("SMS Messages Handled", "Done with #" + message.arg1);
          }
          
          stopSelf(message.arg1);
      }
   };
   
   /**
    * Class for clients to access.  Because we know this service always
    * runs in the same process as its clients, we don't need to deal with
    * IPC.
    */
   public class LocalBinder extends Binder 
   {
      PassengerService getService()
      { return PassengerService.this; }
   }

   @Override
   public IBinder onBind(Intent arg0)
   {
      return m_Binder;
   }

   @Override
   public void onCreate()
   {
      HandlerThread thread = 
         new HandlerThread("PassengerService", Process.THREAD_PRIORITY_BACKGROUND);
      thread.start();
      
      m_ServiceLooper = thread.getLooper();
      m_SMSHandler = new SMSHandler(m_ServiceLooper);
   }

   @Override
   public void onDestroy()
   {
      m_ServiceLooper.quit();
      disableSpeaking();
   }

   @Override
   public void onStart(Intent intent, int startId)
   { }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId)
   {
      Message msg = m_SMSHandler.obtainMessage();
      msg.arg1 = startId;
      msg.arg2 = flags;
      msg.obj = intent.getExtras();
      m_SMSHandler.sendMessage(msg);
      Log.i("ServiceStartArguments", "Sending: " + msg);

      return intent.getBooleanExtra("redeliver", false)
            ? START_REDELIVER_INTENT : START_NOT_STICKY;
   }

   /**
    * Enables speaking of new text messages
    */
   public void enableSpeaking()
   {
      startTTS();
   }

   /**
    * Disables speaking of new text messages
    */
   public void disableSpeaking()
   {
      synchronized (m_TextToSpeech)
      {
         m_TTSInitialized = false;
         m_TextToSpeech.shutdown();
         m_TextToSpeech = null;
      }
   }
   
   /**
    * Starts the TextToSpeech engine
    */
   private void startTTS()
   {
      if (m_TextToSpeech != null)
      {
         m_TextToSpeech.shutdown();
         m_TextToSpeech = null;
      }
      
      m_TextToSpeech = new TextToSpeech(getApplicationContext(), this);
   }

   /**
    * Speaks the messages from an SMS Broadcast
    * 
    * @param context Current Application Context
    * @param messages The SMS Messages to speak aloud
    */
   private void speakMessages(Context context, SmsMessage[] messages)
   {
      if (m_TTSInitialized)
      {
         for (SmsMessage message : messages)
         {
            String messageBody = message.getMessageBody();
            String messageSender = message.getOriginatingAddress();
            
            String contactName = getContactName(context, messageSender);
            
            if (messageBody != null && contactName != null)
            {
               HashMap<String,String> ttsArguments = new HashMap<String,String>();
               ttsArguments.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                       String.valueOf(AudioManager.STREAM_NOTIFICATION));
               
               m_TextToSpeech.speak(
                     String.format(
                           getString(R.string.new_message_text), contactName, messageBody), 
                           TextToSpeech.QUEUE_ADD, ttsArguments);
            }
         }
      }
   }

   /**
    * Retrieves the full name of a contact using the message sender (a phone 
    * number)
    * 
    * @param context Current Application Context
    * @param messageSender A string containing the message sender's phone number
    * @return If the message sender exists in the address book, the sender's full name. 
    *    Otherwise, the sender's phone number
    */
   private String getContactName(Context context, String messageSender)
   {
      if (messageSender == null)
         return getString(R.string.unknown_contact);
      
      Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(messageSender));
      Cursor cursor = context.getContentResolver().query(
            uri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
      
      String contactName = null;
      while (cursor.moveToNext()) 
      { 
         contactName = 
            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
      }
      
      return (contactName == null) ? messageSender.substring(1) : contactName;
   }
   

   /**
    * 
    * @param intentExtras Extras from an SMS Broadcast Intent
    * @return an array of SmsMessages pulled from an SMS Broadcast
    */
   private static SmsMessage[] getMessagesFromBundle(Bundle intentExtras)
   {
      SmsMessage retMsgs[] = null;

      try
      {
         Object pdus[] = (Object[]) intentExtras.get("pdus");
         retMsgs = new SmsMessage[pdus.length];
         for (int n = 0; n < pdus.length; n++)
         {
            byte[] byteData = (byte[]) pdus[n];
            retMsgs[n] = SmsMessage.createFromPdu(byteData);
         }
      }
      catch (Exception e)
      {
         Log.e("GetMessages", "Exception while attempting to parse messages", e);
      }
      
      return retMsgs;
   }
   
   @Override
   public void onInit(int status)
   {
      if (status == TextToSpeech.SUCCESS)
      {
         m_TextToSpeech.setLanguage(Locale.US);
         m_TTSInitialized = true;
      }
      else
         Log.e("TTS Initialization Failure", "Status = " + status);
   }

}
