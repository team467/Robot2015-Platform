package org.usfirst.frc.team467.robot;

import org.apache.log4j.Logger;

import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.Talon;

public class TankDrive implements Driveable
{
    private static final Logger LOGGER = Logger.getLogger(TankDrive.class);
    
    SpeedController fl;
    SpeedController fr;
    SpeedController bl;
    SpeedController br;

    private TankDrive(SpeedController fl, SpeedController fr, SpeedController bl, SpeedController br)
    {
        this.fl = fl;
        this.fr = fr;
        this.bl = bl;
        this.br = br;
    }
    
    public static TankDrive makeTalonTank(int fl, int fr, int bl, int br)
    {
        Talon flMotor = new Talon(fl);
        Talon frMotor = new Talon(fr);
        Talon blMotor = new Talon(bl);
        Talon brMotor = new Talon(br);
        return new TankDrive(flMotor, frMotor, blMotor, brMotor);
    }
    
    public static TankDrive makeJaguarTank(int fl, int fr, int bl, int br)
    {
        Jaguar flMotor = new Jaguar(fl);
        Jaguar frMotor = new Jaguar(fr);
        Jaguar blMotor = new Jaguar(bl);
        Jaguar brMotor = new Jaguar(br);
        return new TankDrive(flMotor, frMotor, blMotor, brMotor);
    }
    
    private double square(double number)
    {
        if (number >= 0.0)
        {
            return number * number;
        }
        else
        {
            return -(number * number);
        }
    }
    
    private void drive(double leftSpeed, double rightSpeed)
    {
        LOGGER.info("leftSpeed=" + (int)(100*leftSpeed) + " rightSpeed=" + (int)(100*rightSpeed));
        fl.set(square(-leftSpeed));
        fr.set(square(rightSpeed));
        bl.set(square(-leftSpeed));
        br.set(square(rightSpeed));
    }


    @Override
    public void turnDrive(double speed)
    {
        drive(speed, -speed);
    }

    @Override
    public void oneStickDrive(Joystick467 joystick, boolean fieldAlign)
    {
        final double turn = joystick.getTankTurn();
        final double speed = joystick.getTankSpeed();
        final double left;
        final double right;
        LOGGER.debug("turn=" + turn + " speed=" + speed);
        
        if (speed > 0.0) {
            if (turn > 0.0)
            {
              left = speed - turn;
              right = Math.max(speed, turn);
            }
            else
            {
              left = Math.max(speed, -turn);
              right = speed + turn;
            }
        }
        else
        {
            if (turn > 0.0) {
              left = -Math.max(-speed, turn);
              right = speed + turn;
            } else {
              left = speed - turn;
              right = -Math.max(-speed, -turn);
            }
        }
        drive(left, right);
    }

    @Override
    public void twoStickDrive(Joystick467 joystickLeft, Joystick467 joystickRight)
    {
        final double speedLeft = joystickLeft.getTankSpeed();
        final double speedRight = joystickRight.getTankSpeed();
        LOGGER.debug("twoStickDrive speedLeft=" + speedLeft + " speedRight=" + speedRight);
        drive(speedLeft, speedRight);
    }

    @Override
    public void individualSteeringDrive(double angle, int steeringId)
    {
        ; // Not applicable.
    }

    @Override
    public void stop()
    {
        drive(0.0, 0.0);
    }

    @Override
    public void unwind()
    {
        ; // Not applicable.
    }

    @Override
    public void strafeDrive(Direction direction)
    {
        double speed = 0.6;
        switch (direction)
        {
            case FRONT:
                // Nothing to do, speed good as is.
                break;
            case BACK:
                speed = -speed;
                break;
            default:
                speed = 0.0;
                return;
        }
        drive(-speed, -speed);
    }

    @Override
    public void revolveDriveLarge(Direction direction)
    {
        // Not applicable

    }

    @Override
    public void revolveDriveSmall(Direction direction)
    {
        // Not applicable

    }

    @Override
    public void individualWheelDrive(double speed, int steeringId)
    {
        // Not Applicable

    }

}
