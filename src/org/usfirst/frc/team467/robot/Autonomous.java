package org.usfirst.frc.team467.robot;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.usfirst.frc.team467.robot.BallRollers.RollerDirection;
import org.usfirst.frc.team467.robot.DriverStation2015.Speed;
import org.usfirst.frc.team467.robot.TBar.tBarDirection;

import edu.wpi.first.wpilibj.Ultrasonic;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import org.apache.log4j.Logger;

public class Autonomous
{
    private static final Logger LOGGER = Logger.getLogger(Autonomous.class);

    private static Autonomous autonomous = null;

    Gyro2016 gyro = null;    
    private Driveable drive = null;
    private VisionProcessor vision = null;
    private Ultrasonic ultrasonic = null;
    
    private TBar tbar = null;

    BallRollers roller = new BallRollers(3, 4);
    
    long actionStartTimeMS = -1;

    /**
     * Run the said action while the condition returns true.
     */
    private interface WhileCondition
    {
        boolean call();
    }
    
    /**
     * Should Always contain:<br>
     * • Lifter<br>
     * • Claw<br>
     * • Drive
     */
    private interface Action
    {
        void call();
    }

    private static class Activity
    {
        // The WhileCondition should return true until the action returns false.
        private final String description;
        private final WhileCondition whileCondition;
        private final Action action;

        private Activity(String description, WhileCondition whileCondition, Action action)
        {
            this.description = description;
            this.whileCondition = whileCondition;
            this.action = action;
        }

        public String getDescription()
        {
            return description;
        }

        void doIt()
        {
            action.call();
        }

        boolean isDone()
        {
            return !whileCondition.call();
        }
    }

    List<Activity> actions = new LinkedList<Activity>();

    private void addAction(String description, WhileCondition whileCondition, Action action)
    {
        actions.add(new Activity(description, whileCondition, action));
    }

    // Returns true for durationSecs, then returns false.
    private boolean forDurationSecs(float durationSecs)
    {
        LOGGER.debug("forDurationSecs durationSecs=" + durationSecs);
        if (actionStartTimeMS < 0)
        {
            resetActionStartTime();
        }
        LOGGER.debug("forDurationSecs actionStartTimeMS=" + actionStartTimeMS);

        final long now = System.currentTimeMillis();
        final long durationMS = (long) (1000 * durationSecs);
        return now < actionStartTimeMS + durationMS;
    }
    
    private boolean whileWidestNotCentered(int marginOfError)
    {
        if (!vision.isEnabled())
        {
            return true;
        }
            
        try
        {
            List<VisionProcessor.Contour> contours = vision.getContours();
            VisionProcessor.Contour contour;
            contour = Collections.max(contours, new VisionProcessor.WidthComp());
            final double centerX = contour.getCenterX();
            final double delta = Math.abs(centerX - vision.getHorizontalCenter());
            LOGGER.debug("untilCentered centerY=" + centerX + " delta=" + delta);
            return delta > marginOfError;
        }
        catch (Exception e)
        {
            LOGGER.info("Missed contour: " + e);
            return true;
        }
    }
    
    private void resetActionStartTime()
    {
        actionStartTimeMS = System.currentTimeMillis();
    }

    // Always returns false, indicates is never done.
    private boolean forever()
    {
        // Never done.
        return true;
    }

    /**
     * Gets a singleton instance of the Autonomous
     * 
     * @return Autonomous object
     */
    public static Autonomous getInstance()
    {
        if (autonomous == null)
        {
            autonomous = new Autonomous(VisionProcessor.getInstance(), Gyro2016.getInstance());
        }
        return autonomous;
    }
    
    public void setDrive(Driveable drive)
    {
        this.drive = drive;
    }

    public void setUltrasonic(Ultrasonic ultra)
    {
        this.ultrasonic = ultra;
    }

    /**
     * Private constructor to setup the Autonomous
     * @param gyro2 
     */
    private Autonomous(VisionProcessor vision, Gyro2016 gyro)
    {
        // TODO Change drive, claw, and lifter to generics implementing respective interfaces
        this.vision = vision;
        this.gyro = gyro;
    }

    /**
     * Sets up the periodic function
     */
    public void initAutonomous()
    {
//        AutoType autonomousType = DriverStation2015.getInstance().getAutoType();
        AutoType autonomousType = AutoType.STAY_IN_PLACE;
        LOGGER.info("AUTO MODE " + autonomousType);

        // Reset actions.
        actions.clear();
        resetActionStartTime();
        
        // Set up gyro and create actions list.
        switch (autonomousType)
        {
            case CROSS_BARRIER:
                initCrossBarrier();
                break;
            case SALLY_PORT:
                initSallyPort();
                break;
            case PORTCULLIS:
                initPortcullis();
                break;
            case DRAWBRIDGE:
                initDrawBridge();
                break;
            case DRIVE_ONLY:
                initDriveOnly();
                break;
            case AIM:
                initAim();
                break;
            case HIGH_GOAL:
                initHighGoal();
                break;
            case STAY_IN_PLACE:
                initStayInPlace();
                break;
            default:
                initStayInPlace();
                break;
        }

        LOGGER.info("Beginning action: " + actions.get(0).getDescription());
    }
    
    private void initCrossBarrier()
    {
        //Moves over barriers using the tilt gyrometer to detect when the robot is going up, down, or driving flat
        //Moves towards the walls using the ultrasonic sensor, and turns (counter)clockwise with the yaw gyrometer to line up for the low shoot
        addAction("Move while gyro is up",
                () -> gyro.isUp(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, -0.7);
                });
        addAction("Move while gyro is flat or when gyro is down",
                () -> gyro.isFlat()|| gyro.isDown(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, -0.7);
                });
        if (gyro.shouldTurnLeft(0)){
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnLeft(0),
                    () -> {
                        drive.turnDrive(0.4);
                    });
        }else{
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnRight(0),
                    () -> {
                        drive.turnDrive(-0.4);
                    });
        }
        addAction("Move while the Ultrasonic is more than 5 feet from the castle",
                () -> ultrasonic.getRangeInches() > 60,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (clockwise)",
                () -> gyro.shouldTurnRight(80), 
                () -> {
                    drive.turnDrive(-0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 12 inches from the wall",
                () -> ultrasonic.getRangeInches() > 12,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(10), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 3 feet from the wall",
                () -> ultrasonic.getRangeInches() > 36,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(-40), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction ("Drive fowards while robot is not flat",
                () -> !gyro.isUp(),
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Shoot the low goal",
                () -> gyro.getYawAngle() == -40 && gyro.getTiltAngle() > 4.0,
                () -> {
                    drive.stop();
                    roller.runRoller(RollerDirection.OUT);
                });
        addAction("Done",
                () -> forever(), 
                () -> {
                    drive.stop();
                });
    }
    
    private void initSallyPort(){
        addAction("turns backwards",
                () -> gyro.shouldTurnLeft(170),
                () -> {
                    drive.turnDrive(0.5);
                });
        addAction("Move while gyro is flat, up, or down",
                () -> gyro.isFlat() || gyro.isUp() || gyro.isDown(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        addAction("Move TBar arm up while the robot is 3 feet from the port",
                () -> ultrasonic.getRangeInches() == 36,
                () -> {
                    drive.stop();
                    tbar.launchTBar(tBarDirection.UP);
                });
        addAction("Move 4 feet away from the port and keep the bar down",
                () -> ultrasonic.getRangeInches() < 46,
                () -> {
                    drive.arcadeDrive(0.0, -0.4);
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("Turn to the left to open the door",
                () -> gyro.shouldTurnLeft(-60),
                () -> {
                    drive.turnDrive(-0.4);
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("Move backwards",
                () -> forDurationSecs(1.0f),
                () -> {
                    drive.arcadeDrive(0.0, 0.35);
                });
        addAction("Turn right to straighten up",
                () -> gyro.shouldTurnRight(-10),
                () -> {
                    drive.turnDrive(-0.4);
                    tbar.launchTBar(tBarDirection.UP);
                });
        addAction("Move while gyro is flat or down",
                () -> gyro.isFlat() ||  gyro.isDown(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        if (gyro.shouldTurnLeft(0)){
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnLeft(0),
                    () -> {
                        drive.turnDrive(0.4);
                    });
        }else{
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnRight(0),
                    () -> {
                        drive.turnDrive(-0.4);
                    });
        }
        addAction("Move while the Ultrasonic is more than 5 feet from the tower",
                () -> ultrasonic.getRangeInches() > 60,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (clockwise)",
                () -> gyro.shouldTurnRight(80), 
                () -> {
                    drive.turnDrive(-0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 12 inches from the wall",
                () -> ultrasonic.getRangeInches() > 12,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(10), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 3 feet from the wall",
                () -> ultrasonic.getRangeInches() > 36,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn (counterclockwise)",
                () -> gyro.shouldTurnLeft(-40), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction ("Drive fowards while robot is not flat",
                () -> !gyro.isUp(),
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Shoot the low goal",
                () -> gyro.getYawAngle() == -40 && gyro.getTiltAngle() > 4.0,
                () -> {
                    drive.stop();
                    roller.runRoller(RollerDirection.OUT);
                });
        addAction("Done",
                () -> forever(), 
                () -> {
                    drive.stop();
                    roller.stop();
                    tbar.stop();
                });
    }
    
    private void initPortcullis()
    {
        // Start facing the wall in front of an item (container or tote), pick it up
        // and carry it rolling backwards to the auto zone.
        //.reset(GyroResetDirection.FACE_TOWARD);// reset to upfield
        
        addAction("turns backwards",
                () -> gyro.shouldTurnLeft(170),
                () -> {
                    drive.turnDrive(0.5);
                });
        addAction("Move while gyro is flat",
                () -> gyro.isFlat(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        addAction("while robot is not flat, lower TBar",
                () -> !gyro.isFlat(),
                () -> {
                    drive.stop();
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("move backwards",
                () -> forDurationSecs(0.7f),
                () -> {
                    drive.arcadeDrive(0.0, 0.4);
                });
        addAction("lift bar",
                () -> gyro.isUp(),
                () -> {
                    tbar.launchTBar(tBarDirection.UP);
                });
        addAction("Move while gyro is up",
                () -> gyro.isUp(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        addAction("Move while gyro is flat or when gyro is down",
                () -> (gyro.isFlat()|| gyro.isDown()) && forDurationSecs(1.0f),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        if (gyro.shouldTurnLeft(0)){
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnLeft(0),
                    () -> {
                        drive.turnDrive(0.4);
                    });
        }else{
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnRight(0),
                    () -> {
                        drive.turnDrive(-0.4);
                    });
        }
        addAction("Move while the Ultrasonic is more than 5 feet from the castle",
                () -> ultrasonic.getRangeInches() > 60,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (clockwise)",
                () -> gyro.shouldTurnRight(80), 
                () -> {
                    drive.turnDrive(-0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 12 inches from the wall",
                () -> ultrasonic.getRangeInches() > 12,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(10), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 3 feet from the wall",
                () -> ultrasonic.getRangeInches() > 36,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(-40), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction ("Drive fowards while robot is not flat",
                () -> !gyro.isUp(),
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Shoot the low goal",
                () -> gyro.getYawAngle() == -40 && gyro.getTiltAngle() > 4.0,
                () -> {
                    drive.stop();
                    roller.runRoller(RollerDirection.OUT);
                });
        addAction("Done",
                () -> forever(), 
                () -> {
                    drive.stop();
                    tbar.stop();
                    roller.stop();
                });
    }
    
    private void initDrawBridge()
    {
        addAction("turns backwards",
                () -> gyro.shouldTurnLeft(170),
                () -> {
                    drive.turnDrive(0.5);
                });
        addAction("Move while gyro is flat, up, or down",
                () -> gyro.isFlat() || gyro.isUp() || gyro.isDown(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                });
        addAction("Move TBar arm up while the robot is 4 feet from the drawbridge",
                () -> ultrasonic.getRangeInches() == 46,
                () -> {
                    drive.stop();
                    tbar.launchTBar(tBarDirection.UP);
                });
        addAction("approach",
                () -> ultrasonic.getRangeInches() > 24,
                () -> {
                    drive.arcadeDrive(0.0, 0.4);
                    tbar.launchTBar(tBarDirection.UP);

                });
       
        addAction("lower bar", 
                () -> ultrasonic.getRangeInches() == 24,
                () -> {
                    drive.stop();
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("open door",
                () -> ultrasonic.getRangeInches() < 48,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("move ahead",
                () -> (gyro.isDown() || gyro.isFlat() || gyro.isUp() ) && forDurationSecs(2.0f),
                () -> {
                    drive.arcadeDrive(0.0, 0.6);
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        if (gyro.shouldTurnLeft(0)){
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnLeft(0),
                    () -> {
                        drive.turnDrive(0.4);
                        tbar.launchTBar(tBarDirection.UP);
                    });
        }else{
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnRight(0),
                    () -> {
                        drive.turnDrive(-0.4);
                        tbar.launchTBar(tBarDirection.UP);
                    });
        }
        addAction("Move while the Ultrasonic is more than 5 feet from the castle",
                () -> ultrasonic.getRangeInches() > 60,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (clockwise)",
                () -> gyro.shouldTurnRight(80), 
                () -> {
                    drive.turnDrive(-0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 12 inches from the wall",
                () -> ultrasonic.getRangeInches() > 12,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(10), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 3 feet from the wall",
                () -> ultrasonic.getRangeInches() > 36,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(-40), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction ("Drive fowards while robot is not flat",
                () -> !gyro.isUp(),
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Shoot the low goal",
                () -> gyro.getYawAngle() == -40 && gyro.getTiltAngle() > 4.0,
                () -> {
                    drive.stop();
                    roller.runRoller(RollerDirection.OUT);
                });
        addAction("Done",
                () -> forever(), 
                () -> {
                    drive.stop();
                    tbar.stop();
                    roller.stop();
                });
    }
    
    private void initChevalDeFrise()
    {
        addAction("turns backwards",
                () -> gyro.shouldTurnLeft(170),
                () -> {
                    drive.turnDrive(0.5);
                });
        addAction("move while flat",
                () -> gyro.isFlat(),
                () -> {
                    drive.arcadeDrive(0.0, 0.5);
                    tbar.launchTBar(tBarDirection.UP);
                });
        addAction("while up",
                () -> gyro.isDown(),
                () -> {
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("Move while gyro is up or down",
                () -> gyro.isUp() || gyro.isDown(),
                () -> {
                    LOGGER.debug("Gyro angle: " + gyro.getTiltAngle());
                    drive.arcadeDrive(0.0, 0.7);
                    tbar.launchTBar(tBarDirection.DOWN);
                });
        addAction("move while flat and raise bar",
                () -> gyro.isFlat(),
                () -> {
                    drive.arcadeDrive(0.0, 0.7);
                    tbar.launchTBar(tBarDirection.UP);
                });
        if (gyro.shouldTurnLeft(0)){
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnLeft(0),
                    () -> {
                        drive.turnDrive(0.4);
                        tbar.launchTBar(tBarDirection.UP);
                    });
        }else{
            addAction("Turn to zero degrees",
                    () -> gyro.isFlat() && gyro.shouldTurnRight(0),
                    () -> {
                        drive.turnDrive(-0.4);
                        tbar.launchTBar(tBarDirection.UP);
                    });
        }
        addAction("Move while the Ultrasonic is more than 5 feet from the castle",
                () -> ultrasonic.getRangeInches() > 60,
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Turn 90 degrees (clockwise)",
                () -> gyro.shouldTurnRight(80), 
                () -> {
                    drive.turnDrive(-0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 12 inches from the wall",
                () -> ultrasonic.getRangeInches() > 12,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(10), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction("Move ahead while the robot is more than 3 feet from the wall",
                () -> ultrasonic.getRangeInches() > 36,
                () -> {
                    drive.arcadeDrive(0.0, -0.3);
                });
        addAction("Turn 90 degrees (counterclockwise)",
                () -> gyro.shouldTurnLeft(-40), 
                () -> {
                    drive.turnDrive(0.5);
                    LOGGER.debug("GYRO YAW ANGLE = " + gyro.getYawAngle());
                });
        addAction ("Drive fowards while robot is not flat",
                () -> !gyro.isUp(),
                () -> {
                    drive.arcadeDrive(0.0, -0.5);
                });
        addAction("Shoot the low goal",
                () -> gyro.getYawAngle() == -40 && gyro.getTiltAngle() > 4.0,
                () -> {
                    drive.stop();
                    roller.runRoller(RollerDirection.OUT);
                });
        addAction("Done",
                () -> forever(), 
                () -> {
                    drive.stop();
                    tbar.stop();
                    roller.stop();
                });
    }
    
    private void initHighGoal()
    {
        final int marginOfError = 30;
        
        addAction("Rotate while square with widest is not centered",
                () -> forever(),
                () -> {
                    seekWidestContour(marginOfError);
                });
        addAction("Shoot the high goal",
                () -> seekAngle(marginOfError),
                ()-> {
                    roller.out(1.0);
                });
        addAction("Stop driving",
                () -> forever(),
                () -> {
                    drive.stop();
                });
    }

    private void initDriveOnly()
    {
//        Gyro2015.getInstance().reset(GyroResetDirection.FACE_LEFT);// reset to upfield
        // Drive to auto zone. Starts on the very edge and just creeps into the zone
        Gyro2016.getInstance();
        addAction("Drive into auto zone", 
                () -> forDurationSecs(2.0f), 
                () -> {
                    roller.stop();
                    tbar.stop();
                    drive.arcadeDrive(0.0, 0.5);
                });
        addAction("Stop driving", 
                () -> forever(), 
                () -> {
                    roller.stop();
                    tbar.stop();
                    drive.stop();
                });
    }

    private void initStayInPlace()
    {
        // Stay in place. Reset to upfield.
//        Gyro2015.getInstance().reset();
          Gyro2016.getInstance();
        addAction("Stop driving", 
                () -> forever(), 
                () -> {
                    roller.stop();
//                    tbar.stop();
                    drive.stop();
                });
    }
    
    private void initAim()
    {
        final int marginOfError = 30;
        
        addAction("Rotate while square with widest is not centered",
//                () -> untilWidestCentered(marginOfError),
                () -> forever(),
                () -> {
                    seekWidestContour(marginOfError);
                });
        
        addAction("Stop driving",
                () -> forever(),
                () -> {
                    drive.stop();
                });
    }

    private void seekWidestContour(int marginOfError)
    {
        final boolean targetIsCentered = seekAngle(marginOfError);
        SmartDashboard.putString("DB/String 8", "Centered: " + targetIsCentered);
        if (targetIsCentered)
        {
            approach(40);
        }
    }
    
    /**
     * 
     * @param marginOfError
     * @return if robot successfully centered on target
     */
    private boolean seekAngle(int marginOfError)
    {
        if (!vision.isEnabled())
        {
            drive.stop();
            return false;
        }
        
//        final double minTurnSpeed = Double.parseDouble(SmartDashboard.getString("DB/String 3", "0.0"));
//        final double maxTurnSpeed = Double.parseDouble(SmartDashboard.getString("DB/String 4", "0.0"));
        final double minTurnSpeed = 0.3; // Double.parseDouble(SmartDashboard.getString("DB/String 3", "0.0"));
        final double maxTurnSpeed = 0.45; // Double.parseDouble(SmartDashboard.getString("DB/String 4", "0.0"));
        final double turnSpeedRange = maxTurnSpeed - minTurnSpeed;
        final double horizontalCenter = vision.getHorizontalCenter();
        
        LOGGER.debug("start seekWidestContour() minTurnSpeed=" + minTurnSpeed + " maxTurnSpeed=" + maxTurnSpeed);
        List<VisionProcessor.Contour> contours = vision.getContours();
        LOGGER.debug("Found " + contours.size() + " contours");
        
        if (contours.size() == 0)
        {
            // Still haven't found what I'm looking for
            drive.stop();
            return false;
        }
        // Find the widest contour
        VisionProcessor.Contour widest = Collections.max(contours, new VisionProcessor.WidthComp());
        final double centerX = widest.getCenterX();
        final double delta = Math.abs(centerX - horizontalCenter);
        LOGGER.debug("Found widest contour, centerX=" + centerX + " delta=" + delta);
        if (delta < marginOfError)
        {
            // Found target
            return true;
        }
//        if (widest.getCenterX() - vision.getHorizontalCenter()) 
        int direction = widest.getCenterX() > horizontalCenter ? -1 : 1;
        final double turnSpeed = direction * (minTurnSpeed + turnSpeedRange * (delta/horizontalCenter));
        drive.turnDrive(turnSpeed);
        LOGGER.info("Turned with turnSpeed " + turnSpeed);
        LOGGER.debug("end seekWidestContour()");
        
        // Target seen but not centered
        return false;
    }
    
    /**
     * 
     * @param desiredDistance in inches
     */
    private void approach(double desiredDistance)
    {
        final double measuredDistance = ultrasonic.getRangeInches();
        final double delta = Math.abs(measuredDistance - desiredDistance);
        if (delta > 12)
        {
            if (measuredDistance > desiredDistance)
            {
                drive.strafeDrive(Direction.FRONT);
            }
            else
            {
                drive.strafeDrive(Direction.BACK);
            }
        }
        else
        {
            drive.stop();
        }
    }
    
    /**
     * Updates the Autonomous routine. Called by Robot.autonomousPeriodic().
     */
    public void updateAutonomousPeriodic()
    {
        // Make sure there something to do.
        if (actions.isEmpty())
        {
            LOGGER.debug("No more actions");
            return;
        }

        Activity currentAction = actions.get(0);
        if (currentAction.isDone())
        {
            // This action is done. Remove it from the list and prepare the next one.
            LOGGER.info("Finished action: " + currentAction.getDescription());
            actions.remove(0);
            resetActionStartTime();
            if (actions.isEmpty())
            {
                // Ran out of actions. We're done.
                return;
            }

            // Move on to the next action.
            currentAction = actions.get(0);
            LOGGER.info("Beginning action: " + currentAction.getDescription());
        }

        LOGGER.debug("Running action: " + currentAction.getDescription() + " number of actions: " + actions.size());
        currentAction.doIt();
    }

    /**
     * Type of Autonomous drive to use.
     *
     */
    enum AutoType
    {
        NO_AUTO, AIM, DRIVE_ONLY, STAY_IN_PLACE, PORTCULLIS, DRAWBRIDGE, SALLY_PORT, CROSS_BARRIER, HIGH_GOAL
    }
}
