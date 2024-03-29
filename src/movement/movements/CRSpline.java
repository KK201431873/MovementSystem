package movement.movements;

import java.util.ArrayList;
import java.util.Arrays;

import movement.util.BoundedDisplacementCalculator;
import movement.util.DisplacementCalculator;
import movement.util.Movement;
import movement.util.MovementType;
import movement.util.Pose;
import teamcode_util.DriveConstants;

public class CRSpline extends Movement {

	private double distance, time;
	private double[] lengths, times, partialTimes, props, partialProps;
	private Pose[] poses;
	private DisplacementCalculator dispCalculator;
	private BoundedDisplacementCalculator[] turnCalculators;
	private double[] correctedHeadings;

	public CRSpline(ArrayList<Pose> poses) {
		this.MOVEMENT_TYPE = MovementType.DRIVE;
		this.poses = poses.stream().toArray(Pose[]::new);
		init();
	}
	
	// primary methods

	public double getLength() {
		return poses.length;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public ArrayList<Pose> getPoses() {
		return new ArrayList<>(Arrays.asList(poses));
	}

	@Override
	public Pose getPose(double elapsedTime) {
		int n = getLocalSegment(elapsedTime);
		double p_r = getLocalProportion(elapsedTime);
		
		Pose pose = getPose(n, p_r);

		int turnIndex = n;
		while (turnCalculators[turnIndex] == null) turnIndex--;
		double heading = correctedHeadings[turnIndex] + turnCalculators[turnIndex].getDisplacement(elapsedTime - partialTimes[turnIndex]);
		
		return new Pose(pose.getX(), pose.getY(), heading);
	}

	@Override
	public Pose getVelocityPose(double elapsedTime) {
		// get theta
		int n = getLocalSegment(elapsedTime);
		double p_r = getLocalProportion(elapsedTime);
		Pose derivative = getDerivative(n, p_r);
		
		double theta = Math.atan2(derivative.getY(), derivative.getX());
		double speed = dispCalculator.getVelocity(elapsedTime);

		int turnIndex = n;
		while (turnCalculators[turnIndex] == null) turnIndex--;
		double angularVelocity = -turnCalculators[turnIndex].getVelocity(elapsedTime - partialTimes[turnIndex]);
		
		return new Pose(
				speed * Math.cos(theta),
				speed * Math.sin(theta),
				angularVelocity
		);
	}
	
	@Override
	public double getTime() {
		return time;
	}
	
	@Override
	public Pose getStartPose() {
		return poses.length>0 ? poses[0] : null;
	}

	@Override
	public Pose getEndPose() {
		return poses.length>0 ? poses[poses.length-1] : null;
	}
	
	public Pose getPose(int index) {
		if (index < 0 || poses.length-1 < index)
			throw new RuntimeException(String.format("Index %s outside of [%s,%s]", index, 0, poses.length-1));
		return poses[index];
	}
	
	public Pose getPose(int segment, double t) {
		if (segment < 0 || poses.length-2 < segment)
			throw new RuntimeException(String.format("Segment index %s outside of [%s,%s]", segment, 0, poses.length-2));

		Pose p0 = poses[Math.max(0, segment-1)];
		Pose p1 = poses[segment];
		Pose p2 = poses[segment + 1];
		Pose p3 = poses[Math.min(poses.length-1, segment+2)];
		
		double tt = t*t;
		double ttt = tt*t;

		double q0 = -ttt + 2*tt - t;
		double q1 = 3*ttt - 5*tt + 2;
		double q2 = -3*ttt + 4*tt + t;
		double q3 = ttt - tt;

		double tx = 0.5 * (p0.getX()*q0 + p1.getX()*q1 + p2.getX()*q2 + p3.getX()*q3);
		double ty = 0.5 * (p0.getY()*q0 + p1.getY()*q1 + p2.getY()*q2 + p3.getY()*q3);
		
		return new Pose(tx, ty, 0);
	}
	
	public Pose getDerivative(int segment, double t) {
		if (segment < 0 || poses.length-2 < segment)
			throw new RuntimeException(String.format("Segment index %s outside of [%s,%s]", segment, 0, poses.length-2));

		Pose p0 = poses[Math.max(0, segment-1)];
		Pose p1 = poses[segment];
		Pose p2 = poses[segment + 1];
		Pose p3 = poses[Math.min(poses.length-1, segment+2)];
		
		double tt = t*t;

		double q0 = -3*tt + 4*t - 1;
		double q1 = 9*tt - 10*t;
		double q2 = -9*tt + 8*t + 1;
		double q3 = 3*tt - 2*t;

		double tx = 0.5 * (p0.getX()*q0 + p1.getX()*q1 + p2.getX()*q2 + p3.getX()*q3);
		double ty = 0.5 * (p0.getY()*q0 + p1.getY()*q1 + p2.getY()*q2 + p3.getY()*q3);
		
		return new Pose(tx, ty, 0);
	}
	
	public int getLocalSegment(double elapsedTime) {
		elapsedTime = bound(elapsedTime, 0, time);
		
		double dx = dispCalculator.getDisplacement(elapsedTime);
		double p_x = distance!=0 ? dx / distance : 0;
		
		int n = 0;
		while (n+1 < partialProps.length && p_x >= partialProps[n+1]) n++;
		
		return n;
	}
	
	public double getLocalProportion(double elapsedTime) {
		double dx = dispCalculator.getDisplacement(elapsedTime);
		int n = getLocalSegment(elapsedTime);
		
		double delta_t = DriveConstants.delta_t;
		double p_r = 0;
		double localDisplacement = 0;
		Pose lastPose = getPose(n,0);
		while (localDisplacement < dx - partialProps[n] * distance) {
			p_r += delta_t;
			Pose currentPose = getPose(n, p_r);
			localDisplacement += Math.hypot(currentPose.getX()-lastPose.getX(), currentPose.getY()-lastPose.getY());
			lastPose = currentPose;
		}
		
		return p_r;
	}
	
	public String toString() {
		String res = "[";
		for (int i = 0; i < poses.length; i++)
			res += String.format("%s%s", poses[i], (i==poses.length-1 ? "" : ", "));
		return res + "]";
	}
	
	private static double bound(double x, double lower, double upper) {
		return Math.max(lower, Math.min(upper, x));
	}
	
	private void init() {
		
		lengths = new double[Math.max(0, poses.length-1)];

		// calculate distance
		distance = 0;
		double delta_t = DriveConstants.delta_t;
		double x = poses[0].getX();
		double y = poses[0].getY();
		for (int i = 0; i < poses.length-1; i++) {
			double length = 0;
			for (double t = 0; t <= 1; t += delta_t) {
				// integrate distances over time
				Pose currentPose = getPose(i, t);
				double deltaDistance = Math.hypot(currentPose.getX()-x, currentPose.getY()-y);
				distance += deltaDistance;
				length += deltaDistance;
				x = currentPose.getX();
				y = currentPose.getY();
			}
			lengths[i] = length;
		}

		dispCalculator = new DisplacementCalculator(distance, DriveConstants.MAX_VELOCITY, DriveConstants.MAX_ACCELERATION);
		time = dispCalculator.getTime();

		turnCalculators = new BoundedDisplacementCalculator[Math.max(0, poses.length-1)];
		
		times = new double[Math.max(0, poses.length-1)];
		partialTimes = new double[Math.max(0, poses.length-1)];
		props = new double[Math.max(0, poses.length-1)];
		partialProps = new double[Math.max(0, poses.length-1)];
		
		correctedHeadings = new double[poses.length];
		correctedHeadings[0] = poses[0].getHeading();
		
		// calculate props, time
		double partialLength = 0;
		double partialTime = 0;
		for (int i = 0; i < lengths.length; i++) {
			// calculate proportions
			partialProps[i] = partialLength / distance;
			props[i] = lengths[i] / distance;
			partialLength += lengths[i];
			
			// calculate times
			double currentPartialTime = dispCalculator.getElapsedTime(partialLength);
			partialTimes[i] = partialTime;
			times[i] = currentPartialTime - partialTime;
			partialTime = currentPartialTime;
		}
		
		// create turn calculators
		double h = poses[0].getHeading();
		double corrected_h = poses[0].getHeading();
		double MAV = DriveConstants.MAX_ANGULAR_VELOCITY;
		double MAA = DriveConstants.MAX_ANGULAR_ACCELERATION;
		for (int i = 0; i < lengths.length; i++) {
			// create turn calculator
			int index = i;
			double corrected_delta_h = normalizeAngle(poses[i+1].getHeading() - corrected_h);
			
			// skip if no change in heading
			if (corrected_delta_h == 0) {
				turnCalculators[index] = new BoundedDisplacementCalculator(0, 1, 1, 1);
				correctedHeadings[index+1] = corrected_h;
				continue;
			}
			
			// get max time available for completing turn
			double maxTime = 0;
			double delta_h = normalizeAngle(poses[i+1].getHeading() - h);
			do {
				// get change in heading since last
				h += delta_h;
				maxTime += times[i];
				correctedHeadings[i] = correctedHeadings[index];
				
				// set non-turning segments to null
				turnCalculators[i] = null;
				
				i++;
				if (i == lengths.length) break;
				delta_h = normalizeAngle(poses[i+1].getHeading() - h);
			}
			while (i < lengths.length && delta_h == 0);
			i--;
			
			// create turn calculator for the original segment, bounded by the max time
			if (i == lengths.length-1) {
				// it is the last segment, make sure robot reaches final pose
				DisplacementCalculator turnTimer = new DisplacementCalculator(corrected_delta_h, MAV, MAA);
				times[times.length-1] += Math.max(0, turnTimer.getTime() - maxTime);
				time += Math.max(0, turnTimer.getTime() - maxTime);
				maxTime = turnTimer.getTime();
			}
			turnCalculators[index] = new BoundedDisplacementCalculator(corrected_delta_h, maxTime, MAV, MAA);
			
			// update heading values for next iteration
			corrected_h += turnCalculators[index].getTotalDisplacement();
			correctedHeadings[i+1] = corrected_h;
		}

		// debug print
//		for (double i : correctedHeadings) System.out.print(i+" ");
//		System.out.println();
		
	}

    /**
     * Normalizes a given angle to [-180,180) degrees.
     * @param degrees the given angle in degrees.
     * @return the normalized angle in degrees.
     */
    private double normalizeAngle(double degrees) {
        double angle = degrees;
        while (angle <= -Math.PI) //TODO: opMode.opModeIsActive() && 
            angle += 2*Math.PI;
        while (angle > Math.PI) //TODO: opMode.opModeIsActive() && 
            angle -= 2*Math.PI;
        return angle;
    }

}
