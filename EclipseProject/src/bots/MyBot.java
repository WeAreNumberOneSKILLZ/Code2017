 package bots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import pirates.City;
import pirates.Drone;
import pirates.Island;
import pirates.Location;
import pirates.MapObject;
import pirates.Pirate;
import pirates.PirateBot;
import pirates.PirateGame;

public class MyBot implements PirateBot {
	public static final int k_SAIL_RANGE = 2;
	public static final int k_ATTACK_RANGE = 3;
	public static final int k_SUPPORT_RANGE = 4;
	public static final int k_CONQUER_RANGE = 3;
	public static final int k_THREAT_RANGE = 6;
	public static List<PirateOrders> orders = new LinkedList<PirateOrders>();
	
	public static double dist(Location loc1, Location loc2){
		return Math.sqrt(Math.pow(loc1.col - loc2.col, 2) + Math.pow(loc1.row - loc2.row, 2));
	}
	public static Pirate getClosestPirate(Location loc, PirateGame game){
		Pirate closest = null;
		for(Pirate enemy : game.getAllEnemyPirates()){
			if(closest == null) closest = enemy;
			else if(MyBot.dist(enemy.location, loc) < MyBot.dist(closest.location, loc))
				closest = enemy;
		} 
		return closest;
		
	}
	public void doTurn(PirateGame game) {
		
		game.debug("turn - " + game.getTurn());
		General.distribute(game);
		General.update(game);
		/*	if(orders.size() == 0) for(int i = 0; i < game.getAllMyPirates().size(); i++){
				PirateOrders order = new PirateOrders(i, game.getAllIslands().get(i % game.getAllIslands().size()),
						PirateOrders.PirateOrderType.attack, game);
				orders.add(order);
			}
			for(int i = 0; i < game.getAllMyPirates().size(); i++){
				PirateOrders order = orders.get(i);
				if(order.isComplete()) {
					game.debug("Order complete! id - " + i);
				}
				order.update(game);
			}*/
		
	}
	/*
	private static void updateEnemyPaths(PirateGame game){
		if(enemyPaths == null) {
			enemyPaths = new HashMap<Integer, List<Location>>();
			for(int i = 0; i < game.getAllEnemyPirates().size(); i++) 
				enemyPaths.put(i, new ArrayList<Location>());
		}
		enemyPaths.forEach((id, path) -> path.add(game.getEnemyPirateById(id).getLocation()));
	}
	public static Map<Integer, List<Location>> getEnemyPaths(){
		return enemyPaths;
	}*/
	
}
/**
 * Order class for a given pirate.
 * @author Yoav
 */
class PirateOrders{
	private Pirate subject; // The Pirate Subject to the orders
	private boolean completed = false; // Indicates whether the order is complete
	private List<Location> path; // Path of the pirate
	private BiConsumer<PirateGame, PirateOrders> order; // Order body
	private MapObject destination; // Destination object the subject should reach
	private int subjectId; // Id of the subject
	
	/**
	 * Builds a PirateOrder. 
	 * @param subjectId - the subject id ({@link Pirate}) on whom the order was given. 
	 * @param destination - the destination which the subject need to arrive at. 
	 * @param initialOrder - The initial function that describes the order.
	 * @param game - the {@link PirateGame}
	 */
	public PirateOrders(int subjectId, MapObject destination, BiConsumer<PirateGame, PirateOrders> initialOrder, PirateGame game){
		this.subject = game.getMyPirateById(subjectId);
		this.subjectId = subjectId;
		path = new ArrayList<Location>();
		this.destination = destination;
		updateOrderType(initialOrder);
	}
	/**
	 * Clears the {@link Location} List that represents the path of the pirate
	 */
	public void clearPath(){
		path.clear();
	}
	/**
	 * Updates the order given to the pirate
	 * @param order - The function that describes the order. 
	 */
	public void updateOrderType(BiConsumer<PirateGame, PirateOrders> order){
		this.order = order;
		this.completed = false;
	}
	/**
	 * Called periodically, updates subject
	 * @param game - a given {@link PirateGame}
	 */
	public void update(PirateGame game){
		//if(!game.getMyLivingAircrafts().contains(subject)) return;
		
		subject = game.getMyPirateById(this.subjectId);
		path.add(subject.location);
		

		order.accept(game, this);
		
	}
	/**
	 * Gets String representation of the order function for comparing with other orders
	 * @return String representation of the order function
	 */
	public String getOrderType(){
		return this.order.toString();
	}
	/**
	 * Gets Subject's Path
	 * @return The Path of the subject
	 */
	public List<Location> getPath() {
		return path;
	}
	
	/**
	 * Gets the Subject's destination
	 * @return The destination of the subject
	 */
	public MapObject getDestination() {
		return destination;
	}
	/**
	 * Gets the subject's id
	 * @return The id of the subject
	 */
	public int getSubjectId() {
		return subjectId;
	}
	
	/**
	 * Updates destination for designated pirate
	 * @param destination - the destination of the pirate ship. 
	 */
	public void updateDestination(MapObject destination){
		this.destination = destination;
		this.completed = false;
	}
	/**
	 * Checks if order is complete
	 * @return A boolean that represents if the order is complete.
	 */
	public boolean isComplete(){
		return this.completed;
	}
	
	/**
	 * A collection of convenient pirate orders.
	 * @author Yoav
	 */
	public static class PirateOrderType{
		
		/**
		 * Orders when attacking. Called periodically. 
		 */
		public static BiConsumer<PirateGame, PirateOrders> attack = (game, pirateOrder) -> {
			List<Location> possibleLocations = new ArrayList<Location>();
			for(int i = -MyBot.k_SAIL_RANGE; i <= MyBot.k_SAIL_RANGE; i++) 
				for(int j = -(MyBot.k_SAIL_RANGE - Math.abs(i)); j <= MyBot.k_SAIL_RANGE - Math.abs(i); j++){
				possibleLocations.add(new Location(pirateOrder.subject.location.row + j,
						pirateOrder.subject.location.col + i));
			} // List all possible locations
			
			Comparator<Location> rankLocation = (loc1, loc2) -> { // Sort & prioritize possible locations
				// Note: Pirate will not avoid contact with enemy pirates if the fight is winnable or will result in a draw
				int score = 0;
				Random rand = new Random(); // Random element to confuse enemies. 
				
				boolean loc1Compromised = false;
				boolean loc2Compromised = false;
				
				Location dest = pirateOrder.destination.getLocation();
				if(loc1.distance(dest) > loc2.distance(dest)) score+=4;
				
				else if(loc1.distance(dest) != loc2.distance(dest)) score-=4;
				
				for(Pirate enemy : game.getAllEnemyPirates()) // Calc danger levels for loc1
					if(loc1.inRange(enemy, MyBot.k_ATTACK_RANGE)) {score+=4; loc1Compromised = true;} 	
				for(Pirate ally : game.getAllMyPirates()) // Calc ally support for loc1
					if(loc1Compromised && !ally.equals(pirateOrder.subject) && loc1.inRange(ally, MyBot.k_SUPPORT_RANGE)) score --;	
				for(Pirate enemy : game.getAllEnemyPirates()) // Calc danger levels for loc2
					if(loc2.inRange(enemy, MyBot.k_ATTACK_RANGE)) {score-=4; loc2Compromised = true;}
				for(Pirate ally : game.getAllMyPirates()) // Calc ally support for loc2
					if(loc2Compromised && !ally.equals(pirateOrder.subject) && loc2.inRange(ally, MyBot.k_SUPPORT_RANGE)) score ++;	
				//if(!loc1Compromised && !loc2Compromised) if(rand.nextInt()%2 == 0) score--; else score++;
				return score;
			};
			Pirate toAttack = null;
			int enemyCount = 0;
			for(Pirate enemy : game.getAllEnemyPirates()){ // Attack if enemy is in range.
				
				if(pirateOrder.subject.inAttackRange(enemy) && enemy.isAlive()){
					if(toAttack == null) toAttack = enemy;
					enemyCount++;
				}
				
			}
			if(toAttack != null){
				//game.debug(pirateOrder.subject + " Attack -> " + toAttack);
				game.attack(pirateOrder.subject, toAttack);
				return;
			}
			
			for(Drone enemyDrone : game.getEnemyLivingDrones()){ // Attack drones if subject is close to destination
				if(pirateOrder.subject.inAttackRange(enemyDrone) && pirateOrder.subject.distance(pirateOrder.destination) < 4){
					game.attack(pirateOrder.subject, enemyDrone);
					return;
				
				}
			}
			
			
			possibleLocations.remove(pirateOrder.subject.location); // Remove current location from list of possible locations
			possibleLocations.sort(rankLocation); // Sort locations and set sail to most optimal location
			game.setSail(pirateOrder.subject, possibleLocations.get(0)); 
			if(pirateOrder.destination.distance(pirateOrder.subject) <= MyBot.k_CONQUER_RANGE) pirateOrder.completed = true; // Check whether order is complete
			//game.debug(String.valueOf(pirateOrder.subjectId) + "attack");
		}; 
		/**
		 * Orders when defending. Called periodically. 
		 */
		public static BiConsumer<PirateGame, PirateOrders> defend = (game, pirateOrder) -> {
			
			List<Location> possibleLocations = new ArrayList<Location>();
			for(int i = -MyBot.k_SAIL_RANGE; i <= MyBot.k_SAIL_RANGE; i++) 
				for(int j = -(MyBot.k_SAIL_RANGE - Math.abs(i)); j <= MyBot.k_SAIL_RANGE - Math.abs(i); j++){
				possibleLocations.add(new Location(pirateOrder.subject.location.row + j,
						pirateOrder.subject.location.col + i));
			} // List all possible locations
			final Pirate closestEnemy = MyBot.getClosestPirate(pirateOrder.destination.getLocation(), game);
			game.debug(closestEnemy);
			Comparator<Location> rankLocation = (loc1, loc2) -> { // Prepare to defend from closest pirate
				int score = 0;
				if(MyBot.dist(closestEnemy.location, loc1) > MyBot.dist(closestEnemy.location, loc2)) score++;
				else score--;
				if(loc2.inRange(pirateOrder.destination, MyBot.k_CONQUER_RANGE)) score+=2;
				if(loc1.inRange(pirateOrder.destination, MyBot.k_CONQUER_RANGE)) score-=2;
				
				return score;
			};
			for(Pirate enemy : game.getAllEnemyPirates()){ // Attack if enemy is in range. 
				if(pirateOrder.subject.inAttackRange(enemy)){
					game.attack(pirateOrder.subject, enemy);
					game.debug(pirateOrder.subject + " Attack -> " + enemy);
					if(!pirateOrder.subject.isAlive()) pirateOrder.completed = true; // Check whether order is complete
					return;
				}
			}
			if(!pirateOrder.subject.inRange(closestEnemy, MyBot.k_THREAT_RANGE)){
				for(Drone enemyDrone : game.getEnemyLivingDrones()){
					if(pirateOrder.subject.inAttackRange(enemyDrone)){
						game.attack(pirateOrder.subject, enemyDrone);
						game.debug(pirateOrder.subject + " Attack -> " + enemyDrone);
						if(!pirateOrder.subject.isAlive()) pirateOrder.completed = true; // Check whether order is complete
						return;
					}
				}
			}
			possibleLocations.sort(rankLocation); // Sort locations and set sail to most optimal location
			game.setSail(pirateOrder.subject, possibleLocations.get(0)); 
			if(!pirateOrder.subject.isAlive()) pirateOrder.completed = true; // Check whether order is complete
			game.debug(String.valueOf(pirateOrder.subjectId) + "defend");
		}; 
		
	}
}
/**
 * Main control class
 * @author Yoav
 */
class General{
	public static List<PirateOrders> orders = new LinkedList<PirateOrders>();
	private static boolean initialized = false;
	private static List<MapObject> targets = new LinkedList<MapObject>();
	private static List<MapObject> requestingSupport = new LinkedList<MapObject>();
	/**
	 * Initializes initial Pirate orders. 
	 * @param game - The Pirategame
	 */
	private static void initialize(PirateGame game){
		if(initialized) return;
		for(Island island : game.getAllIslands()) targets.add((MapObject)island); 
		for(City city : game.getMyCities()) targets.add((MapObject)city);
		initializeOrders(game);
		initialized = true;
	}
	/**
	 * Initialize initial {@link PirateOrders}
	 * @param game - The {@link PirateGame}
	 */
	private static void initializeOrders(PirateGame game){
		for(int i = 0; i < game.getAllMyPirates().size(); i++){
			PirateOrders order = new PirateOrders(i, targets.get(i % targets.size()),
					PirateOrders.PirateOrderType.attack, game);
			orders.add(order);
		}
	}
	/**
	 * Gives pirates periodic orders
	 * @param game - The {@link PirateGame}
	 */
	public static void update(PirateGame game){
		for(PirateOrders order : orders) order.update(game);
	}
	/**
	 * Redistribute orders ingame
	 * @param game - The {@link PirateGame}
	 */
	public static void distribute(PirateGame game){
		initialize(game);
		for(PirateOrders order : orders){
			if(order.isComplete()){
				if(order.getOrderType().equals(PirateOrders.PirateOrderType.attack.toString())){
					order.updateOrderType(PirateOrders.PirateOrderType.defend);
				}
				else if(order.getOrderType().equals(PirateOrders.PirateOrderType.defend.toString())){
					order.updateOrderType(PirateOrders.PirateOrderType.attack);
				}
			}
			/*if(order.getOrderType().equals(PirateOrders.PirateOrderType.defend.toString()) &&
					!order.getDestination().inRange(MyBot.getClosestPirate(
							order.getDestination().getLocation(), game), MyBot.k_THREAT_RANGE)){
				for(MapObject islandToHelp : requestingSupport){
					
				}
			}*/// TODO finish support system
		}
	}
	
	
}

