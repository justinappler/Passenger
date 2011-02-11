package com.passenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * SMSBroadcastReceiver
 * 
 * When an SMS Broadcast is sent, passes the intent to the Passenger Service
 * 
 * @author Justin C. Appler
 * @version 02/10/2011
 */
public class SMSBroadcastReceiver extends BroadcastReceiver
{
   @Override
   public void onReceive(Context context, Intent intent)
   {
      Log.i("SMSBroadcastReceiver", "Received new SMS");
      context.startService(
            new Intent(context, PassengerService.class).putExtras(intent));
   }
}
