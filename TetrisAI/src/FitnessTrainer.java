import jswarm.*;

public class FitnessTrainer extends FitnessFunction{
	private static final int NUM_OF_TRIALS = 1;
	public double evaluate(final double position[]) {
		double ret = 0;
		for(int i = 0; i < NUM_OF_TRIALS; i++){
			ret += train(position);
		}
		ret = ret/NUM_OF_TRIALS;
		System.out.println("lines: "+ ret);
		return ret;
	}
	
	private int train(double position[]){
		State s = new State();
		PlayerSkeleton p = new PlayerSkeleton(position);
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
		}
		return s.getRowsCleared();
	}
	public static void main(String[] args){
		Swarm swarm = new Swarm(30, new AIMyParticle(), new FitnessTrainer());

//		// Use neighborhood
//		Neighborhood neigh = new Neighborhood1D(Swarm.DEFAULT_NUMBER_OF_PARTICLES, true);
//		swarm.setNeighborhood(neigh);
//		swarm.setNeighborhoodIncrement(0.9);

		// Set position (and velocity) constraints. I.e.: where to look for solutions
		swarm.setInertia(1);
		swarm.setMaxPosition(15);
		swarm.setMinPosition(-16);
		swarm.setMaxMinVelocity(0.1);
//		double initial[] = {-7.899265427351652, -4.500158825082766, 
//				3.4181268101392694, -3.3855972247263626,-3.2178882868487753,-9.348695305445199};
//		swarm.setBestPosition(initial);
//		swarm.setBestFitness(151617);
//		Best fitness: 890121.0
//		Best position: 	[-15.0, -7.657493030930552, 15.0, -4.106062205383264, -3.522734028676709, -15.0]

		int numberOfIterations = 50000;

		for (int i = 0; i < numberOfIterations; i++){
			swarm.evolve();
			System.out.println(swarm.toStringStats());
		}
		// Print results
		System.out.println(swarm.toStringStats());
	}
}
