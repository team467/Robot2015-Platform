package org.usfirst.frc.team467.robot;

import org.apache.log4j.Logger;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Talon;

public class TBar
{
    private static final Logger LOGGER = Logger.getLogger(TBar.class);
    
    private Talon tMotor = null;
    
    public TBar(int tMotorChannel)
    {    
        tMotor = new Talon(tMotorChannel); //switch to actual port number
    }
    
    public void stop()
    {
        tMotor.set(0.0);
    }
    
    public void launchTBar(tBarDirection tBarDirection)
    {
        switch(tBarDirection)
        {
            case DOWN:
                tMotor.set(0.4);
                break;
            case UP:
                tMotor.set(-0.4);
                break;
            case STOP:
                stop();
                break;
        }
    }
    enum tBarDirection
    {
        DOWN, UP, STOP
    }

}
