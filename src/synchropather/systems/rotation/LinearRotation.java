package synchropather.systems.rotation;

import synchropather.DriveConstants;
import synchropather.systems.Movement;
import synchropather.systems.StretchedDisplacementCalculator;
import synchropather.systems.TimeSpan;

/**
 * Movement for planning a linear rotation.
 */
public class LinearRotation extends Movement {
	
	private double distance, duration, minDuration;
	private RotationState start, end;
	private TimeSpan timeSpan;
	private StretchedDisplacementCalculator calculator;
	

	/**
	 * Creates a new LinearRotation object with a given start and end RotationState alloted for the given TimeSpan.
	 * @param start
	 * @param end
	 * @param timeSpan
	 */
	public LinearRotation(RotationState start, RotationState end, TimeSpan timeSpan) {
		this.MOVEMENT_TYPE = MovementType.ROTATION;
		this.start = start;
		this.end = end;
		this.timeSpan = timeSpan;
		init();
	}

	@Override
	public double getStartTime() {
		return timeSpan.getStartTime();
	}

	@Override
	public double getEndTime() {
		return timeSpan.getEndTime();
	}

	@Override
	public double getMinDuration() {
		return minDuration;
	}

	@Override
	public double getDuration() {
		return duration;
	}

	/**
	 * @return the indicated RotationState.
	 */
	@Override
	public RotationState getState(double elapsedTime) {
		double t = distance!=0 ? calculator.getDisplacement(elapsedTime) / distance : 0;

		double q0 = 1 - t;
		double q1 = t;

		// linear interpolation
		return start.times(q0).plus(end.times(q1));
	}

	/**
	 * @return the indicated velocity RotationState.
	 */
	@Override
	public RotationState getVelocity(double elapsedTime) {
		double sign = end.minus(start).sign();
		double speed = calculator.getVelocity(elapsedTime);
		
		// scaled velocity vector
		return new RotationState(sign * speed);
	}

	/**
	 * @return the RotationState of this Movement at time zero.
	 */
	@Override
	public RotationState getStartState() {
		return start;
	}

	/**
	 * @return the RotationState reached by the end of this Movement.
	 */
	@Override
	public RotationState getEndState() {
		return end;
	}

	/**
	 * @return "LinearRotation"
	 */
	@Override
	public String getDisplayName() {
		return "LinearRotation";
	}
	
	/**
	 * Calculates total time.
	 */
	private void init() {
		distance = end.minus(start).abs();

		double MAV = DriveConstants.MAX_ANGULAR_VELOCITY;
		double MAA = DriveConstants.MAX_ANGULAR_ACCELERATION;
		
		// create calculator object
		calculator = new StretchedDisplacementCalculator(distance, timeSpan.getDuration(), MAV, MAA);
		
		duration = calculator.getDuration();
		minDuration = calculator.getMinDuration();
		
	}

}