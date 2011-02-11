package com.passenger;

import com.passenger.R;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Passenger Activity
 * 
 * This class displays the main user interface for the Passenger
 * application.
 * 
 * @author Justin C. Appler
 * @version 02/10/2011
 */
public class Passenger extends Activity
{
   private PassengerService m_PassengerService;
   
   private ToggleButton m_ToggleSpeakingButton;
   
   private boolean m_IsServiceBound;
   
   private ServiceConnection m_ServiceConnection = new ServiceConnection() 
   {
       public void onServiceConnected(ComponentName className, IBinder service) 
       {
          m_PassengerService = ((PassengerService.LocalBinder)service).getService();
          
          // Now we've got a service, let's enable the button
          m_ToggleSpeakingButton.setOnClickListener(new ToggleSpeakingListener(m_PassengerService));
          m_ToggleSpeakingButton.setEnabled(true);
       }

       public void onServiceDisconnected(ComponentName className) 
       {
          m_ToggleSpeakingButton.setEnabled(false);
          m_PassengerService = null;
       }
   };
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
      
      m_ToggleSpeakingButton = (ToggleButton) findViewById(R.id.ToggleSpeaking);
      
      doBindService();
   }

   private void doBindService() 
   {
       // Establish a connection with the service.  We use an explicit
       // class name because we want a specific service implementation that
       // we know will be running in our own process (and thus won't be
       // supporting component replacement by other applications).
       bindService(new Intent(Passenger.this, PassengerService.class), m_ServiceConnection, Context.BIND_AUTO_CREATE);
       m_IsServiceBound = true;
   }

   private void doUnbindService() 
   {
       if (m_IsServiceBound) 
       {
           // Detach our existing connection.
           unbindService(m_ServiceConnection);
           m_IsServiceBound = false;
       }
   }

   @Override
   protected void onDestroy() 
   {
       super.onDestroy();
       doUnbindService();
   }
   
   private class ToggleSpeakingListener implements ToggleButton.OnClickListener
   {
      private PassengerService m_PassengerService;
      
      protected ToggleSpeakingListener(PassengerService passService)
      {
         m_PassengerService = passService;
      }
      
      @Override
      public void onClick(View button)
      {
         ToggleButton toggleButton = (ToggleButton) button;
         
         if (toggleButton.isChecked())
         {
            if (m_PassengerService != null)
               m_PassengerService.enableSpeaking();
            else
               Toast.makeText(getApplicationContext(), "Couldn't connect to Passenger Service", 1000).show();
         }
         else
         {
            if (m_PassengerService != null)
               m_PassengerService.disableSpeaking();
            else
               Toast.makeText(getApplicationContext(), "Couldn't connect to Passenger Service", 1000).show();
         }
      }
      
   }
}
