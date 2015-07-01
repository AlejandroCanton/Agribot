package com.example.utd.tryout1;

/**
 * Created by UTD on 6/11/2015.
 */
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client
        implements Runnable
{
    private Socket socket;
    private String ServerIP = "192.168.1.1";
    private static final int ServerPort = 80;
    int i = 0;
    public boolean isConnected = false;
    public boolean hasObstacle = false;
    public String message;

    @Override
    public void run()
    {
        try
        {
            socket = new Socket(ServerIP, ServerPort);
            isConnected = true;

            while (true) {
                BufferedReader in =  new BufferedReader(new InputStreamReader(socket.getInputStream()), 1024);

                //String userInput;
                //while ((userInput = stdIn.readLine()) != null)
                {
                    message = in.readLine();
                    //System.out.println(theMessage);

                    if (message == "hasobstacle")
                    {
                        hasObstacle = true;
                    }
                    else if (message == "noobstacle")
                    {
                        hasObstacle = false;
                    }

                }

            }

        }
        catch(Exception e)
        {
            isConnected = false;
            System.out.print("Whoops! It didn't work!:");
            System.out.print(e.getLocalizedMessage());
            System.out.print("\n");
        }
    }

    public void Send(String s)
    {
        try
        {
            PrintWriter printwriter;

            /*PrintWriter outToServer = new PrintWriter(socket.getOutputStream());
            outToServer.write(s + "\n");
            System.out.print(s + "/n");
            outToServer.flush();*/

            printwriter = new PrintWriter(socket.getOutputStream(),true);
            printwriter.write(s + "\n");  //write the message to output stream

            printwriter.flush();

            //System.out.println(in.read());

        }
        catch (UnknownHostException e) {
            System.out.print(e.toString());
        } catch (IOException e) {
            System.out.print(e.toString());
        }catch (Exception e) {
            System.out.print(e.toString());
        }

    }
}