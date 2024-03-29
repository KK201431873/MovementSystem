package movement.util;

public class BoundedDisplacementCalculator {

	private double distance, time, sign;
	private double MV, MA;
	
	public BoundedDisplacementCalculator(double targetDisplacement, double targetTime, double MV, double MA) {
		this.sign = Math.signum(targetDisplacement);
		this.distance = Math.abs(targetDisplacement);
		this.time = targetTime;
		this.MV = MV;
		this.MA = MA;
		init();
	}
	
	public double getTotalDistance() {
		return distance;
	}
	
	public double getTotalDisplacement() {
		return distance * sign;
	}
	
	public double getTime() {
		return time;
	}
	
	public double getMV() {
		return MV;
	}
	
	public double getMA() {
		return MA;
	}
	
	public double getDisplacement(double elapsedTime) {
		elapsedTime = bound(elapsedTime, 0, time);
		
		double D = Math.abs(this.distance);
		double displacement;
		
		double t_n = time - elapsedTime, t_a = MV/MA;
		if (time <= 2*t_a) {
			// triangle graph
			if (elapsedTime <= time/2)
				displacement = 0.5*MA*elapsedTime*elapsedTime;
			else
				displacement = D - 0.5*MA*t_n*t_n;
		} 
		else {
			// trapezoid graph
			if (elapsedTime <= time/2)
				displacement = 0.5*(elapsedTime + Math.max(0, elapsedTime - t_a))* Math.min(MV, MA*elapsedTime);
			else
				displacement = D - 0.5*(t_n + Math.max(0, t_n - t_a))* Math.min(MV, MA*t_n);
		}
		
		displacement *= sign;
		
		return displacement;
	}
	
	public double getVelocity(double elapsedTime) {
		if (distance == 0) return 0;
		elapsedTime = bound(elapsedTime, 0, time);
		
		double velocity;
		
		double t_n = time - elapsedTime, t_a = MV/MA;
		if (time <= 2*t_a) {
			// triangle graph
			if (elapsedTime <= time/2)
				velocity = MA*elapsedTime;
			else
				velocity = MA*t_n;
		} 
		else {
			// trapezoid graph
			if (elapsedTime <= time/2)
				velocity = Math.min(MV, MA*elapsedTime);
			else
				velocity = Math.min(MV, MA*t_n);
		}
		
		velocity *= sign;
		
		return velocity;
	}
	
	public void init() {
		
		double T = time;
		double t_a = MV/MA;
		double D_max;
		
		if (T < 2*t_a) {
			// triangle graph
			D_max = MA * T * T / 4.0;
		} 
		else {
			// trapezoid graph
			D_max = (T - MV/MA) * MV;
		}
		
		if (distance >= D_max) {
			// no need to compensate MV
			distance = D_max;
		} 
		

//		System.out.println(distance/MV);

		// trim time
		double previousMV = MV;
		MV = Math.min(MV, Math.sqrt(distance*MA));
		
		if (Math.abs(MV-previousMV)<1e-3)
			time = distance/MV + t_a;
		else
			time = Math.sqrt(4*distance/MA);
		
	}
	
	private static double bound(double x, double lower, double upper) {
		return Math.max(lower, Math.min(upper, x));
	}
	
}
