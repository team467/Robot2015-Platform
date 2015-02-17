package org.usfirst.frc.team467.robot;

import java.util.Comparator;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.ImageType;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Autonomous
{

    private static Autonomous autonomous = null;

    private Drive drive = null;
    private Claw claw = null;
    private Lifter lifter = null;

    private AutoType autonomousType = AutoType.GRAB_CONTAINER_PUSH_TOTE;

    long autonomousStartTime = -999999;

    /**
     * Gets a singleton instance of the Autonomous
     * 
     * @return Autonomous object
     */
    public static Autonomous getInstance()
    {
        if (autonomous == null)
            autonomous = new Autonomous();
        return autonomous;
    }

    /**
     * Private constructor to setup the Autonomous
     */
    private Autonomous()
    {
        drive = Drive.getInstance();
        claw = Claw.getInstance();
        lifter = Lifter.getInstance();
    }

    /**
     * Sets what autonomous routine to run. Please do not call this after
     * updateAutonomousPeriodic() has been run.
     * 
     * @param autoType
     */
    public void setAutoType(AutoType autoType)
    {
        this.autonomousType = autoType;
    }

    /**
     * Sets up the periodic function
     */
    public void initAutonomous()
    {
        autonomousStartTime = -999999;
    }

    /**
     * Updates the Autonomous routine.
     */
    public void updateAutonomousPeriodic()
    {
        if (autonomousStartTime < 0)
        {
            autonomousStartTime = System.currentTimeMillis();
        }

        // in milliseconds
        long timeSinceStart = System.currentTimeMillis() - autonomousStartTime;

        //
        // Possible Auto Types:
        //
        // Drive to AUTO zone,
        // Grab box and drive to AUTO zone
        // Grab container and drive to AUTO zone
        // Grab box and grab container and drive to AUTO zone
        switch (this.autonomousType)
        {
            case DRIVE_ONLY:
                driveOnly(timeSinceStart);
                break;
            case GRAB_ITEM:
                grabItem(timeSinceStart);
                break;
            case PUSH_TOTE:
                pushTote(timeSinceStart);
                break;
            case GRAB_CONTAINER_PUSH_TOTE:
                grabContainerPushTote(timeSinceStart);
                break;
            case GRAB_BOTH:
                grabBoth(timeSinceStart);
                break;
            case TEST:
                test(timeSinceStart);
        }
    }

    long gripTimeElapsed = 0;
    long ungripTimeElapsed = 0;
    long lifterTimeElapsed = 0;

    public void test(long timeSinceStart)
    {
        if (timeSinceStart < 1000)
        {
            drive.crabDrive(0, 0.5, false);
        }
        else if (timeSinceStart < 2000)
        {
            drive.crabDrive(Math.PI / 2, 0.5, false);
        }
        else if (timeSinceStart < 3000)
        {
            drive.crabDrive(Math.PI, 0.5, false);
        }
        else if (timeSinceStart < 4000)
        {
            drive.crabDrive(-Math.PI / 2, 0.5, false);
        }
        else
        {
            drive.crabDrive(0, 0, false);
        }
    }

    private void grabItem(long timeSinceStart)
    {
        // Start behind an item (container or tote), and pick it up
        // and carry it to the auto zone

        if (!claw.isClosed())
        {
            // close motor around box
            claw.moveClaw(ClawMoveTypes.GRIP_SLOW);
            gripTimeElapsed = System.currentTimeMillis() - autonomousStartTime;
        }
        else if (timeSinceStart < 500 + gripTimeElapsed)
        {
            lifter.setLift(LiftTypes.LIFT_UP_SLOW);
        }
        else if (timeSinceStart < 3500 + gripTimeElapsed) // in milleseconds
        {
            drive.crabDrive(0, // angle to drive at in radians
                    -0.4,  // speed to drive at in percent
                    false); // no field align
        }
        else
        {
            drive.crabDrive(0, // angle to drive at in radians
                    0,  // speed to drive at in percent
                    false); // no field align
        }
    }

    private void pushTote(long timeSinceStart)
    {
        // Pushes tote into the AUTO zone as we drive ourselves
        // into the auto zone
        if (timeSinceStart < 3000) // in milleseconds
        {
            drive.crabDrive(0, // angle to drive at in radians
                    0.5,  // speed to drive at in percent
                    false); // no field align
        }
        else
        {
            drive.crabDrive(0, // angle to drive at in radians
                    0,  // speed to drive at in percent
                    false); // no field align
        }
    }

    private void driveOnly(long timeSinceStart)
    {
        // Drive to auto zone. Starts on the very edge and
        // just creeps into the zone
        if (timeSinceStart < 2000) // in milleseconds
        {
            drive.crabDrive(0, // angle to drive at in radians
                    0.5,  // speed to drive at in percent
                    false); // no field align
        }
        else
        {
            drive.crabDrive(0, // angle to drive at in radians
                    0,  // speed to drive at in percent
                    false); // no field align

        }

    }

    private void grabContainerPushTote(long timeSinceStart)
    {
        // Pick up container, strafe over to tote,
        // plow tote into AUTO zone while holding container
        if (!claw.isClosed())
        {
            claw.moveClaw(ClawMoveTypes.GRIP_SLOW);
            gripTimeElapsed = System.currentTimeMillis() - autonomousStartTime;
            // grip trash can
        }
        else if (timeSinceStart < 6000 + gripTimeElapsed)
        {
            lifter.setLift(LiftTypes.LIFT_UP_SLOW);

        }
        else if (timeSinceStart < 7000 + gripTimeElapsed)
        {
            lifter.setLift(LiftTypes.NO_LIFT);
            drive.crabDrive(Math.PI / 2, 0.5, false);
            // Strafe right (half speed, one second)
        }
        else if (timeSinceStart < 9000 + gripTimeElapsed)
        {
            drive.crabDrive(0, 0.5, false);
            // ram forward (half speed, two seconds,
            // (one to make up for backwards movement, two to get into auto zone)
        }
        else if (timeSinceStart < 10000 + gripTimeElapsed)
        {
            claw.moveClaw(ClawMoveTypes.UNGRIP_SLOW);
            
        }
        else if (timeSinceStart < 11000 + gripTimeElapsed)
        {
            claw.moveClaw(ClawMoveTypes.STOP);
            drive.crabDrive(0, 0, false);
        }
        else if (timeSinceStart < 12000 + gripTimeElapsed)
        {
            drive.crabDrive(Math.PI, 0.5, false);
        }
        else
        {
            drive.crabDrive(0, 0, false);
        }
        // TODO Change the times between events so they reflect actual measurements
        // Currently using estimated values
    }

    private void grabBoth(long timeSinceStart)
    {
        // Pick up the container and stack it on the tote.
        // Then, pick up the tote and drive the robot and stack into
        // the AUTO zone.
        if (!claw.isClosed())
        {
            claw.moveClaw(ClawMoveTypes.GRIP_SLOW);
            gripTimeElapsed = System.currentTimeMillis() - autonomousStartTime;
        }
        else if (timeSinceStart < gripTimeElapsed + 2000)
        {
            lifter.basicDriveLifter(LifterDirection.UP, false);
        }
        else if (timeSinceStart < gripTimeElapsed + 3000)
        {
            drive.crabDrive(Math.PI / 2, 0.5, false);
        }
        else if (timeSinceStart < gripTimeElapsed + 4000)
        {
            lifter.basicDriveLifter(LifterDirection.DOWN, false);
        }
        else if (timeSinceStart < gripTimeElapsed + 4000 + gripTimeElapsed)
        {
            claw.moveClaw(ClawMoveTypes.UNGRIP_SLOW);

        }
    }

    /**
     * Type of Autonomous drive to use.
     * 
     * @author kyle
     *
     */
    enum AutoType
    {
        TEST, DRIVE_ONLY, GRAB_ITEM, PUSH_TOTE, GRAB_CONTAINER_PUSH_TOTE, GRAB_BOTH
    }
}
