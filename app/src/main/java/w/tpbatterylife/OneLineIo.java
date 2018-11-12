package w.tpbatterylife;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class OneLineIo {

    public static File logFile = null;
    static Context mContext;
    public  OneLineIo(Context context) {
        mContext = context;
    }

        public static Long getValue(File _f) {

            String text = null;

            try {
                FileInputStream fis = new FileInputStream(_f);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr);

                text = br.readLine();

                br.close();
                isr.close();
                fis.close();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
            }

            Long value = null;
            if (text != null) {
                try	{
                    value = Long.parseLong(text);
                } catch (NumberFormatException nfe) 	{
                    Log.e(MainActivity.TAG, nfe.getMessage());
                    value = null;
                }
            }
            return value;
        }

    public static void logString(File logFile, String string) {
        try {
            FileOutputStream fos = new FileOutputStream(logFile,true);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            BufferedWriter bw = new BufferedWriter(osw);

            bw.write(string);

            bw.flush();
            bw.close();
            osw.close();
            fos.close();
        } catch (IOException e) {
            Log.i(MainActivity.TAG, e.getMessage());
        }
    }
}
